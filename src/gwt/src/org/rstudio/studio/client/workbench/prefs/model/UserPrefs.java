/*
 * UserPrefs.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.DeferredInitCompletedEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.events.UserPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.events.UserPrefsChangedHandler;
import org.rstudio.studio.client.common.GlobalDisplay;

@Singleton
public class UserPrefs extends UserPrefsComputed 
   implements UserPrefsChangedHandler, DeferredInitCompletedEvent.Handler
{
   public interface Binder
           extends CommandBinder<Commands, UserPrefs> {}

   @Inject
   public UserPrefs(Session session, 
                  EventBus eventBus,
                  PrefsServerOperations server,
                  SatelliteManager satelliteManager,
                  Commands commands,
                  Binder binder,
                  GlobalDisplay display)
   {
      super(session.getSessionInfo(),
            (session.getSessionInfo() == null ? 
               JsArray.createArray().cast() :
               session.getSessionInfo().getPrefs()));

      session_ = session;
      server_ = server;
      satelliteManager_ = satelliteManager;
      display_ = display;
      commands_ = commands;
      reloadAfterInit_ = false;
      
      binder.bind(commands_, this);

      eventBus.addHandler(UserPrefsChangedEvent.TYPE, this);
      eventBus.addHandler(DeferredInitCompletedEvent.TYPE, this);
   }
   
   public void writeUserPrefs()
   {
      writeUserPrefs(null);
   }

   public void writeUserPrefs(CommandWithArg<Boolean> onCompleted)
   {
      server_.setUserPrefs(
         session_.getSessionInfo().getUserPrefs(),
         new ServerRequestCallback<Void>() 
         {
            @Override
            public void onResponseReceived(Void v)
            {
               UserPrefsChangedEvent event = new UserPrefsChangedEvent(
                     session_.getSessionInfo().getUserPrefLayer());

               if (Satellite.isCurrentWindowSatellite())
               {
                  RStudioGinjector.INSTANCE.getEventBus()
                     .fireEventToMainWindow(event);
               }
               else
               {
                  // let satellites know prefs have changed
                  satelliteManager_.dispatchCrossWindowEvent(event);
               }
               
               if (onCompleted != null)
               {
                  onCompleted.execute(true);
               }
            }
            @Override
            public void onError(ServerError error)
            {
               if (onCompleted != null)
               {
                  onCompleted.execute(false);
               }
               Debug.logError(error);
            }
         });
   }
   
   @Override
   public void onUserPrefsChanged(UserPrefsChangedEvent e)
   {
      syncPrefs(e.getName(), e.getValues());
   }
   
   @Handler
   public void onEditUserPrefs()
   {
      server_.editPreferences(new VoidServerRequestCallback());
   }

   @Handler
   public void onClearUserPrefs()
   {
      display_.showYesNoMessage(GlobalDisplay.MSG_QUESTION,
         "Confirm Clear Preferences",
         "Are you sure you want to clear your preferences? All RStudio settings " +
         "will be restored to their defaults, and your R session will be " +
         "restarted.",
         false,
         (indicator) ->
         {
            server_.clearPreferences(new ServerRequestCallback<String>()
            {
               public void onResponseReceived(String path)
               {
                  indicator.onCompleted();
                  display_.showMessage(
                        GlobalDisplay.MSG_INFO,
                        "Preferences Cleared",
                        "Your preferences have been cleared, and your R session " +
                        "will now be restarted. A backup copy of your preferences " +
                        "can be found at: \n\n" + path,
                        () ->
                        {
                           // Restart R, then reload the UI when done
                           reloadAfterInit_ = true;
                           commands_.restartR().execute();
                        },
                        "Restart R",
                        false);
               }

               @Override
               public void onError(ServerError error)
               {
                  indicator.onError(error.getMessage());
               }
            });
         },
         null,
         "Clear Preferences",
         "Cancel",
         false);
   }

   @Override
   public void onDeferredInitCompleted(DeferredInitCompletedEvent event)
   {
      // Called when R is finished initializing; if we have just cleared prefs,
      // we also reload the UI when R's done restarting
      if (reloadAfterInit_)
      {
         reloadAfterInit_ = false;
         WindowEx.get().reload();
      }
   }
   
   @Handler
   public void onViewAllPrefs()
   {
      server_.viewPreferences(new VoidServerRequestCallback());
   }

   public static final int LAYER_DEFAULT  = 0;
   public static final int LAYER_SYSTEM   = 1;
   public static final int LAYER_COMPUTED = 2;
   public static final int LAYER_USER     = 3;
   public static final int LAYER_PROJECT  = 4;
   
   private final Session session_;
   private final PrefsServerOperations server_;
   private final SatelliteManager satelliteManager_;
   private final GlobalDisplay display_;
   private final Commands commands_;
   private boolean reloadAfterInit_;
}
