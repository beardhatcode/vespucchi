package de.blau.android;

import java.io.InputStream;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.dialogs.Progress;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.ViewBox;
import de.blau.android.resources.DataStyle;
import de.blau.android.util.ACRAHelper;

/**
 * Simple extension around AsyncTask for loading OSM data files or similar which typically have a lot of boilerplate code
 * 
 * @author simon
 *
 */
public abstract class ReadAsyncClass extends AsyncTask<Boolean, Void, ReadAsyncResult> {

    private static final String  DEBUG_TAG = "ReadAsyncClass";
    final Context                context;
    final Map                    map;
    final InputStream            is;
    final boolean                add;
    final PostAsyncActionHandler postLoad;
    final ViewBox                viewBox;
    final boolean                hasActivity;

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     * @param is the InputStream
     * @param add if true add to exiting data (not always used)
     * @param postLoad a handler to call afte the load has completed
     */
    protected ReadAsyncClass(@NonNull final Context context, @NonNull final InputStream is, boolean add, @Nullable final PostAsyncActionHandler postLoad) {
        this.context = context;
        this.is = is;
        this.add = add;
        this.postLoad = postLoad;
        map = context instanceof Main ? ((Main) context).getMap() : null;
        hasActivity = context instanceof FragmentActivity;
        viewBox = App.getLogic().getViewBox();
    }

    @Override
    protected void onPreExecute() {
        if (hasActivity) {
            Progress.showDialog((FragmentActivity) context, Progress.PROGRESS_LOADING);
        }
    }

    @Override
    protected void onPostExecute(ReadAsyncResult result) {
        if (hasActivity) {
            Progress.dismissDialog((FragmentActivity) context, Progress.PROGRESS_LOADING);
        }
        if (map != null) {
            try {
                viewBox.setRatio(map, (float) map.getWidth() / (float) map.getHeight());
            } catch (OsmException e) {
                Log.d(DEBUG_TAG, "onPostExecute got " + e.getMessage());
            }
            DataStyle.updateStrokes(App.getLogic().strokeWidth(viewBox.getWidth()));
        }
        int code = result.getCode();
        if (code != 0) {
            if (code == ErrorCodes.OUT_OF_MEMORY && App.getDelegator().isDirty()) {
                code = ErrorCodes.OUT_OF_MEMORY_DIRTY;
            }
            try {
                if (hasActivity && !((FragmentActivity) context).isFinishing()) {
                    ErrorAlert.showDialog((FragmentActivity) context, code, result.getMessage());
                }
            } catch (Exception ex) { // now and then this seems to throw a WindowManager.BadTokenException,
                ACRAHelper.nocrashReport(ex, ex.getMessage());
            }
            if (postLoad != null) {
                postLoad.onError();
            }
        } else {
            if (postLoad != null) {
                postLoad.onSuccess();
            }
        }
        if (map != null) {
            map.invalidate();
        }
        if (hasActivity) {
            ((FragmentActivity) context).invalidateOptionsMenu();
        }
    }
}
