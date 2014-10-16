/*
 * This file is part of Technic Launcher Core.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launchercore.install;

import net.technicpack.launchercore.exception.PackNotAvailableOfflineException;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.platform.IPlatformApi;

import net.technicpack.utilslib.Utils;
import net.technicpack.utilslib.ZipUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class ModpackInstaller<VersionData> {
    private final IPlatformApi platformApi;
    private final String clientId;

	public ModpackInstaller(IPlatformApi platformApi, String clientId) {
		this.clientId = clientId;
        this.platformApi = platformApi;
	}

	public VersionData installPack(InstallTasksQueue<VersionData> tasksQueue, ModpackModel modpack, String build) throws IOException, InterruptedException {
        modpack.save();
        modpack.initDirectories();

		Version installedVersion = modpack.getInstalledVersion();
        if (installedVersion == null) {
            platformApi.incrementPackInstalls(modpack.getName());
            Utils.sendTracking("installModpack", modpack.getName(), modpack.getBuild(), clientId);
        }

        tasksQueue.runAllTasks();

        Version versionFile = new Version(build, false);
        versionFile.save(modpack.getBinDir());

        return tasksQueue.getCompleteVersion();
    }

	public VersionData prepareOfflinePack(ModpackModel modpack, IVersionDataParser<VersionData> parser) throws IOException, InterruptedException {
        modpack.initDirectories();

		File versionFile = new File(modpack.getBinDir(), "version.json");
		File modpackJar = new File(modpack.getBinDir(), "modpack.jar");

		if (modpackJar.exists()) {
			ZipUtils.extractFile(modpackJar, modpack.getBinDir(), "version.json");
		}

		if (!versionFile.exists()) {
			throw new PackNotAvailableOfflineException(modpack.getDisplayName());
		}

		String json = FileUtils.readFileToString(versionFile, Charset.forName("UTF-8"));
		return parser.parseVersionData(json);
	}
}
