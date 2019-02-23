/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2019  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.ui.download;

import javafx.scene.Node;
import org.jackhuang.hmcl.download.DownloadProvider;
import org.jackhuang.hmcl.download.LibraryAnalyzer;
import org.jackhuang.hmcl.download.RemoteVersion;
import org.jackhuang.hmcl.download.game.LibraryDownloadException;
import org.jackhuang.hmcl.game.Library;
import org.jackhuang.hmcl.game.Version;
import org.jackhuang.hmcl.setting.Profile;
import org.jackhuang.hmcl.task.DownloadException;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.task.TaskResult;
import org.jackhuang.hmcl.ui.Controllers;
import org.jackhuang.hmcl.ui.construct.MessageBox;
import org.jackhuang.hmcl.ui.wizard.WizardController;
import org.jackhuang.hmcl.ui.wizard.WizardProvider;
import org.jackhuang.hmcl.util.StringUtils;

import java.util.Map;

import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

public final class InstallerWizardProvider implements WizardProvider {
    private final Profile profile;
    private final String gameVersion;
    private final Version version;
    private final String forge;
    private final String liteLoader;
    private final String optiFine;

    public InstallerWizardProvider(Profile profile, String gameVersion, Version version) {
        this.profile = profile;
        this.gameVersion = gameVersion;
        this.version = version;

        LibraryAnalyzer analyzer = LibraryAnalyzer.analyze(version);
        forge = analyzer.getForge().map(Library::getVersion).orElse(null);
        liteLoader = analyzer.getLiteLoader().map(Library::getVersion).orElse(null);
        optiFine = analyzer.getOptiFine().map(Library::getVersion).orElse(null);
    }

    public Profile getProfile() {
        return profile;
    }

    public String getGameVersion() {
        return gameVersion;
    }

    public Version getVersion() {
        return version;
    }

    public String getForge() {
        return forge;
    }

    public String getLiteLoader() {
        return liteLoader;
    }

    public String getOptiFine() {
        return optiFine;
    }

    @Override
    public void start(Map<String, Object> settings) {
    }

    @Override
    public Object finish(Map<String, Object> settings) {
        settings.put("success_message", i18n("install.success"));
        settings.put("failure_callback", (FailureCallback) (settings1, exception, next) -> alertFailureMessage(exception, next));

        TaskResult<Version> ret = Task.ofResult(() -> version);

        if (settings.containsKey("forge"))
            ret = ret.thenTaskResult(profile.getDependency().installLibraryAsync((RemoteVersion) settings.get("forge")));

        if (settings.containsKey("liteloader"))
            ret = ret.thenTaskResult(profile.getDependency().installLibraryAsync((RemoteVersion) settings.get("liteloader")));

        if (settings.containsKey("optifine"))
            ret = ret.thenTaskResult(profile.getDependency().installLibraryAsync((RemoteVersion) settings.get("optifine")));

        return ret.then(profile.getRepository().refreshVersionsAsync());
    }

    @Override
    public Node createPage(WizardController controller, int step, Map<String, Object> settings) {
        DownloadProvider provider = profile.getDependency().getDownloadProvider();
        switch (step) {
            case 0:
                return new AdditionalInstallersPage(this, controller, profile.getRepository(), provider);
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public boolean cancel() {
        return true;
    }

    public static void alertFailureMessage(Exception exception, Runnable next) {
        if (exception instanceof LibraryDownloadException) {
            Controllers.dialog(i18n("launch.failed.download_library", ((LibraryDownloadException) exception).getLibrary().getName()) + "\n" + StringUtils.getStackTrace(exception.getCause()), i18n("install.failed.downloading"), MessageBox.ERROR_MESSAGE, next);
        } else if (exception instanceof DownloadException) {
            Controllers.dialog(i18n("install.failed.downloading.detail", ((DownloadException) exception).getUrl()) + "\n" + StringUtils.getStackTrace(exception.getCause()), i18n("install.failed.downloading"), MessageBox.ERROR_MESSAGE, next);
        } else {
            Controllers.dialog(StringUtils.getStackTrace(exception), i18n("install.failed"), MessageBox.ERROR_MESSAGE, next);
        }
    }
}
