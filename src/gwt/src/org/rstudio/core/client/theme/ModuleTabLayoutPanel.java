/*
 * ModuleTabLayoutPanel.java
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
package org.rstudio.core.client.theme;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.events.*;
import org.rstudio.core.client.layout.WindowState;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.DoubleClickState;
import org.rstudio.core.client.widget.ProgressSpinner;
import org.rstudio.core.client.widget.model.ProvidesBusy;
import org.rstudio.studio.client.workbench.events.BusyEvent;
import org.rstudio.studio.client.workbench.events.BusyHandler;

public class ModuleTabLayoutPanel extends TabLayoutPanel
{
   public static class ModuleTab extends Composite implements BusyHandler
   {
      public ModuleTab(String title, ThemeStyles styles, boolean canClose, boolean minimized)
      {
         layoutPanel_ = new HorizontalPanel();
         layoutPanel_.setStylePrimaryName(styles.tabLayout());

         String minimized_id = "";
         if (minimized)
            minimized_id = "_minimized";

         // Assign a unique element ID based on the tab's title
         ElementIds.assignElementId(layoutPanel_.getElement(), 
               ElementIds.WORKBENCH_TAB + minimized_id + "_" + ElementIds.idSafeString(title));

         HTML left = new HTML();
         left.setStylePrimaryName(styles.tabLayoutLeft());
         layoutPanel_.add(left);

         HorizontalPanel center = new HorizontalPanel();
         center.setStylePrimaryName(styles.rstheme_tabLayoutCenter());
         Label label = new Label(title, false);
         center.add(label);
         if (canClose)
         {
            closeButton_ = new Image(new ImageResource2x(ThemeResources.INSTANCE.closeTab2x()));
            closeButton_.setStylePrimaryName(styles.closeTabButton());
            closeButton_.addStyleName(ThemeStyles.INSTANCE.handCursor());
            closeButton_.setAltText("Close " + title + " tab");
            center.add(closeButton_);
         }
         layoutPanel_.add(center);

         HTML right = new HTML();
         right.setStylePrimaryName(styles.tabLayoutRight());
         layoutPanel_.add(right);

         addDomHandler(new MouseDownHandler()
         {
            public void onMouseDown(MouseDownEvent event)
            {
               // Stop double-click of tab from selecting the tab title text
               event.preventDefault();
            }
         }, MouseDownEvent.getType());

         initWidget(layoutPanel_);
      }

      public Widget getWidget()
      {
         return super.getWidget();
      }

      public HandlerRegistration addClickHandler(ClickHandler handler)
      {
         return addDomHandler(handler, ClickEvent.getType());
      }

      public HandlerRegistration addCloseButtonClickHandler(ClickHandler handler)
      {
         return closeButton_.addClickHandler(handler);
      }

      @Override
      public void onBusy(BusyEvent event)
      {
         setBusy(event.isBusy());
      }
      
      private void setBusy(boolean isBusy)
      {
         if (isBusy)
         {
            if (busySpinner_ == null)
            {
               HorizontalPanel center = (HorizontalPanel)closeButton_.getParent();
               
               busySpinner_ = new ProgressSpinner(center.getElement());
               
               busySpinner_.setHeight("9px");
               busySpinner_.setWidth("9px");
               busySpinner_.getElement().getStyle().setMarginLeft(4, Unit.PX);
               busySpinner_.getElement().getStyle().setMarginTop(6, Unit.PX);
               
               if (center != null)
                  center.add(busySpinner_);
            }
            closeButton_.setVisible(false);
            busySpinner_.setVisible(true);
         }
         else
         {
            if (busySpinner_ != null) {
               busySpinner_.removeFromParent();
               busySpinner_ = null;
            }
            closeButton_.setVisible(true);
         }
      }

      private HorizontalPanel layoutPanel_;
      private Image closeButton_;
      private ProgressSpinner busySpinner_;
   }

   public ModuleTabLayoutPanel(final WindowFrame owner)
   {
      super(BAR_HEIGHT, Style.Unit.PX, "Pane");
      owner_ = owner;
      styles_ = ThemeResources.INSTANCE.themeStyles();
      addStyleName(styles_.moduleTabPanel());
      addDomHandler(new MouseDownHandler() {
         public void onMouseDown(MouseDownEvent event)
         {
            if (!isWithinTopBand(event.getNativeEvent()))
               return;
            // Stop click-drag selection from working in top band
            event.preventDefault();
         }
      }, MouseDownEvent.getType());
      addDomHandler(new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            if (!isWithinTopBand(event.getNativeEvent()))
               return;

            event.preventDefault();
            event.stopPropagation();
            if (doubleClickState_.checkForDoubleClick(event.getNativeEvent()))
            {
               owner.fireEvent(new WindowStateChangeEvent(WindowState.MAXIMIZE));
            }
         }
      }, ClickEvent.getType());
   }

   private boolean isWithinTopBand(NativeEvent event)
   {
      int absTop = getAbsoluteTop();
      return absTop + BAR_HEIGHT > event.getClientY();
   }

   @Override
   public void add(Widget child, String text)
   {
      add(child, text, false);
   }

   @Override
   public void add(Widget child, String text, boolean asHtml)
   {
      add(child, text, asHtml, null);
   }

   public void add(Widget child, String text, boolean asHtml, 
                   ClickHandler closeHandler)
   {
      add(child, text, asHtml, closeHandler, null);
   }

   public void add(final Widget child, String text, boolean asHtml,
                   ClickHandler closeHandler, ProvidesBusy providesBusy)
   {
      if (asHtml)
         throw new UnsupportedOperationException("HTML tab names not supported");

      ModuleTab tab = new ModuleTab(text, styles_, closeHandler != null, false /*minimized*/);
      super.add(child, tab);

      if (closeHandler != null)
         tab.addCloseButtonClickHandler(closeHandler);
      
      if (providesBusy != null)
         providesBusy.addBusyHandler(tab);
   }

   @Override
   public void selectTab(int index)
   {
      super.selectTab(Math.max(0, Math.min(index, getWidgetCount() - 1)));
      if (index == 0)
         owner_.addStyleName(styles_.firstTabSelected());
      else
         owner_.removeStyleName(styles_.firstTabSelected());
   }

   @Override
   public void add(Widget child, Widget tab)
   {
      throw new UnsupportedOperationException();
   }

   private final ThemeStyles styles_;

   private final DoubleClickState doubleClickState_ = new DoubleClickState();
   public static final int BAR_HEIGHT = 23;
   private final WindowFrame owner_;
}
