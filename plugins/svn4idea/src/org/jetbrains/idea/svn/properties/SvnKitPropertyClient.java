package org.jetbrains.idea.svn.properties;

import com.intellij.openapi.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.svn.api.BaseSvnClient;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.ISVNPropertyHandler;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import java.io.File;

/**
 * @author Konstantin Kolosovsky.
 */
public class SvnKitPropertyClient extends BaseSvnClient implements PropertyClient {

  @Nullable
  @Override
  public SVNPropertyData getProperty(@NotNull SvnTarget target,
                                     @NotNull String property,
                                     boolean revisionProperty,
                                     @Nullable SVNRevision revision) throws VcsException {
    try {
      if (!revisionProperty) {
        if (target.isFile()) {
          return myVcs.createWCClient().doGetProperty(target.getFile(), property, target.getPegRevision(), revision);
        } else {
          return myVcs.createWCClient().doGetProperty(target.getURL(), property, target.getPegRevision(), revision);
        }
      } else {
        return getRevisionProperty(target, property, revision);
      }
    }
    catch (SVNException e) {
      throw new VcsException(e);
    }
  }

  @Override
  public void getProperty(@NotNull SvnTarget target,
                          @NotNull String property,
                          @Nullable SVNRevision revision,
                          @Nullable SVNDepth depth,
                          @Nullable ISVNPropertyHandler handler) throws VcsException {
    runGetProperty(target, property, revision, depth, handler);
  }

  @Override
  public void list(@NotNull SvnTarget target,
                   @Nullable SVNRevision revision,
                   @Nullable SVNDepth depth,
                   @Nullable ISVNPropertyHandler handler) throws VcsException {
    runGetProperty(target, null, revision, depth, handler);
  }

  private void runGetProperty(@NotNull SvnTarget target,
                              @Nullable String property,
                              @Nullable SVNRevision revision,
                              @Nullable SVNDepth depth,
                              @Nullable ISVNPropertyHandler handler) throws VcsException {
    SVNWCClient client = myVcs.createWCClient();

    try {
      if (target.isURL()) {
        client.doGetProperty(target.getURL(), property, target.getPegRevision(), revision, depth, handler);
      } else {
        client.doGetProperty(target.getFile(), property, target.getPegRevision(), revision, depth, handler, null);
      }
    } catch (SVNException e) {
      throw new VcsException(e);
    }
  }

  private SVNPropertyData getRevisionProperty(@NotNull SvnTarget target, @NotNull final String property, @Nullable SVNRevision revision) throws SVNException{
    final SVNWCClient client = myVcs.createWCClient();
    final SVNPropertyData[] result = new SVNPropertyData[1];
    ISVNPropertyHandler handler = new ISVNPropertyHandler() {
      @Override
      public void handleProperty(File path, SVNPropertyData property) throws SVNException {
        handle(property);
      }

      @Override
      public void handleProperty(SVNURL url, SVNPropertyData property) throws SVNException {
        handle(property);
      }

      @Override
      public void handleProperty(long revision, SVNPropertyData property) throws SVNException {
        handle(property);
      }

      private void handle(@NotNull SVNPropertyData data) {
        if (property.equals(data.getName())) {
          result[0] = data;
        }
      }
    };

    if (target.isFile()) {
      client.doGetRevisionProperty(target.getFile(), null, revision, handler);
    } else {
      client.doGetRevisionProperty(target.getURL(), null, revision, handler);
    }

    return result[0];
  }
}
