/*
 * DirectoryChooserTextBox.java
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.TextBox;

public class DirectoryChooserTextBox extends TextBoxWithButton
{
   public DirectoryChooserTextBox()
   {
      this("", "", null);
   }

   public DirectoryChooserTextBox(String label)
   {
      this(label, "", null); 
   }

   public DirectoryChooserTextBox(String label, 
                                  String emptyLabel, 
                                  Focusable focusAfter)
   {
      this(label, 
           emptyLabel,
           focusAfter,
           RStudioGinjector.INSTANCE.getFileDialogs(),
           RStudioGinjector.INSTANCE.getRemoteFileSystemContext());
   }

   public DirectoryChooserTextBox(String label,
                                  boolean buttonDisabled,
                                  Focusable focusAfter)
   {
      this(label,
            "",
            buttonDisabled,
            focusAfter,
            RStudioGinjector.INSTANCE.getFileDialogs(),
            RStudioGinjector.INSTANCE.getRemoteFileSystemContext());
   }
   
   public DirectoryChooserTextBox(String label, Focusable focusAfter)
   {
      this(label, "", focusAfter);
   }
   
   
   public DirectoryChooserTextBox(String label, 
                                  Focusable focusAfter,
                                  FileDialogs fileDialogs,
                                  FileSystemContext fsContext)
   {
      this(label, "", focusAfter, fileDialogs, fsContext);
   }

   public DirectoryChooserTextBox(String label, 
                                  String emptyLabel,
                                  final Focusable focusAfter,
                                  final FileDialogs fileDialogs,
                                  final FileSystemContext fsContext)
   {
      this(label, emptyLabel, false, focusAfter, fileDialogs, fsContext);
   }

   public DirectoryChooserTextBox(String label,
                                  String emptyLabel,
                                  boolean buttonDisabled,
                                  final Focusable focusAfter,
                                  final FileDialogs fileDialogs,
                                  final FileSystemContext fsContext)
   {
      this(label, emptyLabel, "Browse...", buttonDisabled, focusAfter, fileDialogs, fsContext);
   }
   
   public DirectoryChooserTextBox(String label, 
                                  String emptyLabel,
                                  String browseLabel,
                                  boolean buttonDisabled,
                                  final Focusable focusAfter,
                                  final FileDialogs fileDialogs,
                                  final FileSystemContext fsContext)
   {
      super(label, emptyLabel, browseLabel, null);

      if (buttonDisabled)
      {
         getButton().setEnabled(false);
         setReadOnly(false);

         getTextBox().addChangeHandler(new ChangeHandler()
         {
            public void onChange(ChangeEvent event)
            {
               setText(getTextBox().getText());
            }
         });
      }

      addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            fileDialogs.chooseFolder(
                  "Choose Directory",
                  fsContext,
                  FileSystemItem.createDir(getText()),
                  new ProgressOperationWithInput<FileSystemItem>()
                  {
                     public void execute(FileSystemItem input,
                                         ProgressIndicator indicator)
                     {
                        if (input == null)
                           return;

                        setText(input.getPath());
                        indicator.onCompleted();
                        if (focusAfter != null)
                           focusAfter.setFocus(true);
                     }
                  });
         }
      });
      
   }    
}
