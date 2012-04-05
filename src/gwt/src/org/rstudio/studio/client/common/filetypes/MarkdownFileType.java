/*
 * MarkdownFileType.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.filetypes;

import com.google.gwt.resources.client.ImageResource;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.workbench.commands.Commands;

import java.util.HashSet;

public class MarkdownFileType extends TextFileType
{
   MarkdownFileType(String id,
                    String label,
                    EditorLanguage editorLanguage,
                    String defaultExtension,
                    ImageResource icon)
   {
      super(id, 
            label, 
            editorLanguage, 
            defaultExtension,
            icon,
            true,    // word-wrap
            false, 
            false, 
            false, 
            false, 
            false, 
            false,
            false,
            false);
   }


   @Override
   public HashSet<AppCommand> getSupportedCommands(Commands commands)
   {
      HashSet<AppCommand> result = super.getSupportedCommands(commands);
      
      return result;
   }
}