/*
 * SessionSnippets.cpp
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

#include "SessionSnippets.hpp"

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/Json.hpp>
#include <core/system/Xdg.hpp>

#include <boost/bind.hpp>

#include <session/SessionModuleContext.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace snippets {

using namespace core;

namespace {

FilePath s_snippetsMonitoredDir;

void notifySnippetsChanged()
{
   Error error = core::writeStringToFile(
          s_snippetsMonitoredDir.childPath("changed"),
          core::system::generateUuid());
   if (error)
      LOG_ERROR(error);
}

FilePath getLegacySnippetsDir()
{
   // Hard-coded snippets folder used in RStudio 1.2 and below.
   return module_context::resolveAliasedPath("~/.R/snippets");
}

FilePath getSnippetsDir(bool autoCreate = false)
{
   FilePath snippetsDir = core::system::xdg::userConfigDir().complete("snippets");
   if (autoCreate)
   {
      Error error = snippetsDir.ensureDirectory();
      if (error)
         LOG_ERROR(error);
   }
   return snippetsDir;
}

Error saveSnippets(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)
{
   json::Array snippetsJson;
   Error error = json::readParams(request.params, &snippetsJson);
   if (error)
      return error;

   FilePath snippetsDir = getSnippetsDir(true);
   for (const json::Value& valueJson : snippetsJson)
   {
      if (json::isType<json::Object>(valueJson))
      {
         const json::Object& snippetJson = valueJson.get_obj();
         std::string mode, contents;
         Error error = json::readObject(snippetJson, "mode", &mode,
                                                     "contents", &contents);
         if (error)
         {
            LOG_ERROR(error);
            continue;
         }

         error = writeStringToFile(snippetsDir.childPath(mode + ".snippets"),
                                   contents);
         if (error)
            LOG_ERROR(error);
      }
   }

   notifySnippetsChanged();

   return Success();
}

bool isSnippetFilePath(const FilePath& filePath,
                       std::string* pMode)
{
   if (filePath.isDirectory())
      return false;
   
   if (filePath.extensionLowerCase() != ".snippets")
      return false;
   
   *pMode = boost::algorithm::to_lower_copy(filePath.stem());
   return true;
}

Error getSnippetsAsJson(json::Array* pJsonData)
{
   std::vector<FilePath> dirs;

   // Add system-level snippets files
   dirs.push_back(core::system::xdg::systemConfigDir().complete("snippets"));

   // Add snippets files from older RStudio
   dirs.push_back(getLegacySnippetsDir());

   // Add user snippets files
   dirs.push_back(getSnippetsDir());

   for (const auto& snippetsDir: dirs)
   {
      if (!snippetsDir.exists() || !snippetsDir.isDirectory())
      {
         // Skip if no snippets at this location
         continue;
      }
      
      // Get the contents of each file here, and pass that info back up
      // to the client
      std::vector<FilePath> snippetPaths;
      Error error = snippetsDir.children(&snippetPaths);
      if (error)
         return error;
      
      for (const FilePath& filePath : snippetPaths)
      {
         // bail if this doesn't appear to be a snippets file
         std::string mode;
         if (!isSnippetFilePath(filePath, &mode))
            continue;
         
         std::string contents;
         error = readStringFromFile(filePath, &contents);
         if (error)
            return error;

         // Remove (override) any existing snippets for this mode.
         for (auto it = pJsonData->begin(); it != pJsonData->end(); it++)
         {
            json::Object obj = (*it).get_obj();
            if (obj["mode"].get_str() == mode)
            {
               pJsonData->erase(it);
               break;
            }
         }
         
         // Add the snippets for this mode to the collection
         json::Object snippetJson;
         snippetJson["mode"] = mode;
         snippetJson["contents"] = contents;
         pJsonData->push_back(snippetJson);
      }
   }
   return Success();
}

void checkAndNotifyClientIfSnippetsAvailable()
{
   json::Array jsonData;

   // attempt to get the snippets as a JSON array 
   Error error = getSnippetsAsJson(&jsonData);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   // if we got some, send them to the client
   if (!jsonData.empty())
   {
      ClientEvent event(client_events::kSnippetsChanged, jsonData);
      module_context::enqueClientEvent(event);
   }
}

void onDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (s_snippetsMonitoredDir.empty())
      return;

   if (pDoc->path().empty() || pDoc->dirty())
      return;

   FilePath snippetsDir = getSnippetsDir();
   if (!snippetsDir.exists())
      return;

   // if this was within the snippets dir then
   if (module_context::resolveAliasedPath(pDoc->path()).isWithin(snippetsDir))
      notifySnippetsChanged();
}

void onClientInit()
{
   checkAndNotifyClientIfSnippetsAvailable();
}

void onSnippetsChanged()
{
   checkAndNotifyClientIfSnippetsAvailable();
}

void afterSessionInitHook(bool newSession)
{
   // register to be notified when snippets are changed
   s_snippetsMonitoredDir = module_context::registerMonitoredUserScratchDir(
            "snippets",
            boost::bind(onSnippetsChanged));

   // fire snippet changed when a user edits a snippet directly in the
   // source editor
   source_database::events().onDocUpdated.connect(onDocUpdated);
}

Error getSnippets(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   json::Array jsonData;
   Error error = getSnippetsAsJson(&jsonData);
   if (error)
      return error;

   pResponse->setResult(jsonData);

   return Success();
}

} // anonymous namespace

core::Error initialize()
{
   using namespace module_context;
   using boost::bind;
   
   events().afterSessionInitHook.connect(afterSessionInitHook);
   events().onClientInit.connect(onClientInit);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(registerRpcMethod, "save_snippets", saveSnippets))
         (bind(registerRpcMethod, "get_snippets", getSnippets));
   
   return initBlock.execute();
}

} // end namespace snippets
} // end namespace modules
} // end namespace session
} // end namespace rstudio
