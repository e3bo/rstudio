/*
 * DesktopSessionServersOverlay.cpp
 *
 * Copyright (C) 2019 by RStudio, Inc.
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

#include "DesktopSessionServersOverlay.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

DesktopSessionServers& sessionServers()
{
   static DesktopSessionServers singleton;
   return singleton;
}

DesktopSessionServers::DesktopSessionServers() = default;

void DesktopSessionServers::showSessionServerOptionsDialog()
{
}

LaunchLocationResult DesktopSessionServers::showSessionLaunchLocationDialog()
{
   LaunchLocationResult result;
   return result;
}

QJsonObject SessionServerPathMapping::toJson() const
{
   return QJsonObject();
}

SessionServerPathMapping SessionServerPathMapping::fromJson(const QJsonObject& pathMappingJson)
{
   return SessionServerPathMapping();
}

const std::string& SessionServer::label() const
{
   if (!name().empty())
      return name();
   else
      return url();
}

QJsonObject SessionServer::toJson() const
{
   return QJsonObject();
}

SessionServer SessionServer::fromJson(const QJsonObject& sessionServerJson)
{
   return SessionServer();
}

Error SessionServer::test()
{
   return Success();
}

SessionServerSettings& sessionServerSettings()
{
   static SessionServerSettings singleton;
   return singleton;
}

SessionServerSettings::SessionServerSettings() :
   sessionLocation_(SessionLocation::Ask),
   closeServerSessionsOnExit_(CloseServerSessions::Never)
{
}

ConfigSource SessionServerSettings::configSource() const
{
   return ConfigSource::User;
}

void SessionServerSettings::save(const std::vector<SessionServer>& servers,
                                 SessionLocation sessionLocation,
                                 CloseServerSessions closeServerSessionsOnExit)
{
}

void SessionServerSettings::addSaveHandler(const boost::function<void()>& onSave)
{
}

} // namespace desktop
} // namespace rstudio
