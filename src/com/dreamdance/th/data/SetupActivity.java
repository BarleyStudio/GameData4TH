package com.dreamdance.th.data;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class SetupActivity extends Activity
{
    static final String DEFAULT_CACHE_PATH = "/sdcard/Android/data/com.dreamdance.th.data/cache";
    static final String DEFAULT_INSTALL_PATH = "/sdcard";
    static final String DATA_PACK_NAME = "th.zip";
    String mCachePath;
    String mInstallPath;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        if (!Utility.isSuitableResolution(this)) {
            createResolutionDialog().show();
        }
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            // Show dialog and end
            createExternalStorageUnmountedDialog().show();

        } else {
            Button install = (Button)findViewById(R.id.install_button);
            install.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    extractFile();
                }
            });

            Button uninstall = (Button)findViewById(R.id.uninstall_button);
            uninstall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    removeFiles();
                }
            });
        }
    }

    void extractFile() {
        final ProgressDialog dialog = new ProgressDialog(this);
        final ProgressDialog spin = new ProgressDialog(this);

        final Utility.UnzipTask unzipTask = new Utility.UnzipTask(getInstallPath() + "/") {

              @Override
              protected void onPreExecute() {
                  super.onPreExecute();
                  dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                  dialog.setMessage(getString(R.string.extract_files));
                  dialog.setIndeterminate(false);
                  dialog.setCancelable(false);
                  dialog.show();
              }

              @Override
              protected void onProgressUpdate(Integer... values) {
                  super.onProgressUpdate(values);
                  dialog.setProgress(values[0]);
              }

              @Override
              protected void onPostExecute(AsyncTaskResult<String> result) {
                  super.onPostExecute(result);
                  Exception error;
                  if ((error = result.getError()) != null) {
                      dialog.dismiss();
                      if (error instanceof IOException
                              && error.toString().contains("No space left on device")) {
                          createNotEnoughSpaceDialog().show();
                      } else {
                          createExtractFailedDialog().show();
                      }
                  } else {
                      dialog.hide();
                      if (mSourceFile.delete())
                      Toast.makeText(SetupActivity.this, R.string.install_success, Toast.LENGTH_SHORT).show();
                  }
              }

          };

          AsyncTask<String, Void, AsyncTaskResult<File>> copyTask = new AsyncTask<String, Void, AsyncTaskResult<File>>() {

              @Override
              protected AsyncTaskResult<File> doInBackground(String... params) {

                  try {
                      Utility.copyAsset(SetupActivity.this, params[0], params[1]);
                  } catch (IOException e) {

                      return new AsyncTaskResult<File>(e);
                  }
                  return new AsyncTaskResult<File>(new File(params[1] + "/"
                          + params[0]));
              }

              @Override
              protected void onPreExecute() {
                  super.onPreExecute();    //To change body of overridden methods use File | Settings | File Templates.
                  spin.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                  spin.setMessage(getString(R.string.copy_files));
                  spin.setIndeterminate(false);
                  spin.setCancelable(false);
                  spin.show();

              }

              @Override
              protected void onPostExecute(AsyncTaskResult<File> result) {
                  super.onPostExecute(result);
                  File f;
                  spin.dismiss();
                  if ((f = result.getResult()) != null) {
                      unzipTask.execute(f);
                  } else {
                      createExtractFailedDialog().show();
                  }
              };
          };

        copyTask.execute(DATA_PACK_NAME, getCachePath());
    }

    void removeFiles() {
        if (Utility.checkPathExists(getInstallPath())) {
            File dir = new File(getInstallPath());
            final ProgressDialog dialog = new ProgressDialog(this);
            AsyncTask<File, Void, Integer> removeTask = new AsyncTask<File, Void, Integer>() {

                @Override
                protected Integer doInBackground(File... params) {
                    File dir = params[0];
                    Utility.deleteRecursive(dir);
                    return 0;
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();    //To change body of overridden methods use File | Settings | File Templates.
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setMessage(getString(R.string.deleting));
                    dialog.setIndeterminate(false);
                    dialog.setCancelable(false);
                    dialog.show();
                }

                @Override
                protected void onPostExecute(Integer result) {
                    super.onPostExecute(result);
                    dialog.dismiss();
                    Toast.makeText(SetupActivity.this, R.string.uninstall_success, Toast.LENGTH_SHORT).show();
                };
            };

            removeTask.execute(dir);
        } else {
            Toast.makeText(SetupActivity.this, R.string.noting_to_remove, Toast.LENGTH_SHORT).show();
        }
    }

    Dialog createExternalStorageUnmountedDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.no_external_storage)
                .setCancelable(false)
        .setNeutralButton(R.string.ok,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SetupActivity.this.finish();
                    }

                });

        return builder.create();
    }

    public Dialog createExtractFailedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(
                R.string.extracting_failed)
                .setCancelable(false)
                .setNegativeButton(R.string.quit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //dialogInterface.dismiss();
                        SetupActivity.this.finish();
                    }
                });

        return builder.create();
    }

    public Dialog createNotEnoughSpaceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(
                R.string.not_enough_space)
                .setCancelable(false)
                .setNeutralButton(R.string.quit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //dialog.dismiss();
                        SetupActivity.this.finish();
                    }
                });

        return builder.create();
    }

    public Dialog createResolutionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(
                R.string.not_suitable_resolution)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SetupActivity.this.finish();
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

        return builder.create();
    }

    String getInstallPath() {
        if (null == mInstallPath) {
            File externalFilesDir = Environment.getExternalStorageDirectory();
            String defaultCthPath = null;
            if (null != externalFilesDir) {
                defaultCthPath = externalFilesDir.getAbsolutePath();
            } else {
                defaultCthPath = DEFAULT_INSTALL_PATH;
            }
            mInstallPath = defaultCthPath + "/th";
        }
        return mInstallPath;
    }

    String getCachePath() {
        if (null == mCachePath) {
            File cachePath = getExternalCacheDir();
            if (null != cachePath) {
                mCachePath = cachePath.getAbsolutePath();
            } else {
                mCachePath = DEFAULT_CACHE_PATH;
            }
        }
        return mCachePath;
    }
}
