/*
 * Copyright (C) 2015-2018 Emanuel Moecklin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.deltastream.example.edittextcontroller;

import android.content.Intent;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Toast;

import com.deltastream.example.edittextcontroller.api.RTApi;
import com.deltastream.example.edittextcontroller.api.format.RTFormat;
import com.deltastream.example.edittextcontroller.effects.AbsoluteSizeEffect;
import com.deltastream.example.edittextcontroller.effects.AlignmentEffect;
import com.deltastream.example.edittextcontroller.effects.BoldEffect;
import com.deltastream.example.edittextcontroller.effects.BulletEffect;
import com.deltastream.example.edittextcontroller.effects.Effect;
import com.deltastream.example.edittextcontroller.effects.Effects;
import com.deltastream.example.edittextcontroller.effects.ForegroundColorEffect;
import com.deltastream.example.edittextcontroller.effects.ItalicEffect;
import com.deltastream.example.edittextcontroller.effects.NumberEffect;
import com.deltastream.example.edittextcontroller.effects.SpanCollectMode;
import com.deltastream.example.edittextcontroller.effects.StrikethroughEffect;
import com.deltastream.example.edittextcontroller.effects.SubscriptEffect;
import com.deltastream.example.edittextcontroller.effects.SuperscriptEffect;
import com.deltastream.example.edittextcontroller.effects.UnderlineEffect;
import com.deltastream.example.edittextcontroller.spans.LinkSpan;
import com.deltastream.example.edittextcontroller.spans.RTSpan;
import com.deltastream.example.edittextcontroller.utils.Helper;
import com.deltastream.example.edittextcontroller.utils.Selection;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.deltastream.example.edittextcontroller.RTManager.ToolbarVisibility.*;

/**
 * The RTManager manages the different components:
 * the toolbar(s), the editor(s) and the Activity/Fragment(s) via the RTProxy.
 * <p>
 * Note: the transient modifier is "misused" here to mark variables that
 * are not saved and restored in onSaveInstanceState / onCreate.
 */
public class RTManager implements RTEditTextListener,RTToolbarListener{

    /*
     * Identifies the link dialog / fragment
     */
    private static final String ID_01_LINK_FRAGMENT = "ID_01_LINK_FRAGMENT";

    /*
     * The toolbar(s) may automatically be shown or hidden when a rich text
     * editor gains or loses focus depending on the ToolbarVisibility setting.
     */
    public enum ToolbarVisibility {
        /*
         * Toolbar(s) are shown/hidden automatically depending on whether an
         * editor that uses rich text gains/loses focus.
         *
         * This is the default.
         */
        AUTOMATIC,

        /*
         * Toolbar(s) are always shown.
         */
        SHOW,

        /*
         * Toolbar(s) are never shown.
         */
        HIDE;
    }

    private ToolbarVisibility mToolbarVisibility = AUTOMATIC;

    /*
     * To set the visibility of the toolbar(s) we call setToolbarVisibility(boolean).
     * To change the visibility we start an animation. Before the animation ends
     * setToolbarVisibility() could have been called multiple times with different
     * visibility parameters. We need to make sure once (one of) the animation
     * ends we use the newest visibility status (that's what this variable stands for).
     *
     * We do clear the animation with each call to setToolbarVisibility but if the
     * animation has already started the onAnimationEnd is still called.
     */
    private boolean mToolbarIsVisible;

    /*
     * When an Activity is started (e.g. to pick an image),
     * we need to know which editor gets the result
     */
    private int mActiveEditor = Integer.MAX_VALUE;

    /*
     * This defines what Selection link operations are applied to
     * (inserting, editing, removing links).
     */
    private Selection mLinkSelection;

    /*
     * We need these to delay hiding the toolbar after a focus loss of an editor
     */
    transient private Handler mHandler;
    transient private boolean mIsPendingFocusLoss;
    transient private boolean mCancelPendingFocusLoss;

    /*
     * Map the registered editors by editor id (RTEditText.getId())
     */
    transient private Map<Integer, RTEditText> mEditors;

    /*
     * Map the registered toolbars by toolbar id (RTToolbar.getId())
     */
    transient private Map<Integer, RTToolbar> mToolbars;

    /*
     * That's our link to "the outside world" to perform operations that need
     * access to a Context or an Activity
     */
    transient private RTApi mRTApi;

    /*
     * The RTOperationManager is used to undo/redo operations
     */
    transient private RTOperationManager mOPManager;

    // ****************************************** Lifecycle Methods *******************************************


    public RTManager(RTApi mRTApi) {

        this.mRTApi = mRTApi;

        mHandler = new Handler();
        mEditors = new ConcurrentHashMap<Integer, RTEditText>();
        mToolbars = new ConcurrentHashMap<Integer, RTToolbar>();
        mOPManager = new RTOperationManager();

        EventBus.getDefault().register(this);
        //EventBus.getDefault().register(this);
    }

    /**
     * Called to retrieve per-instance state before being killed so that the
     * state can be restored in the constructor.
     *
     * @param outState Bundle in which to place your saved state.
     */
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("mToolbarVisibility", mToolbarVisibility.name());
        outState.putBoolean("mToolbarIsVisible", mToolbarIsVisible);
        outState.putInt("mActiveEditor", mActiveEditor);
        if (mLinkSelection != null) {
            outState.putSerializable("mLinkSelection", mLinkSelection);
        }
    }

    /**
     * Perform any final cleanup before the component is destroyed.
     *
     * @param isSaved True if the text is saved, False if it's dismissed. This is
     *                needed to decide whether media (images etc.) are to be
     *                deleted.
     */
    public void onDestroy(boolean isSaved) {
        EventBus.getDefault().unregister(this);

        for (RTEditText editor : mEditors.values()) {
            editor.unregister();
            editor.onDestroy(isSaved);
        }
        mEditors.clear();

        for (RTToolbar toolbar : mToolbars.values()) {
            toolbar.removeToolbarListener();
        }
        mToolbars.clear();

        mRTApi = null;
    }

    // ****************************************** Public Methods *******************************************

    /**
     * Register a rich text editor.
     * <p>
     * Before using the editor it needs to be registered to an RTManager.
     * Using means any calls to the editor (setText will fail if the editor isn't registered)!
     * MUST be called from the ui thread.
     *
     * @param editor The rich text editor to register.
     */
    public void registerEditor(RTEditText editor, boolean useRichTextEditing) {

        mEditors.put(editor.getId(), editor);
        editor.register(this);
        editor.setRichTextEditing(useRichTextEditing, false);

        updateToolbarVisibility();

    }



    /**
     * Unregister a rich text editor.
     * <p>
     * This method may be called before the component is destroyed to stop any
     * interaction with the editor. Not doing so may result in (asynchronous)
     * calls coming through when the Activity/Fragment is already stopping its
     * operation.
     * <p>
     * Must be called from the ui thread.
     * <p>
     * Important: calling this method is obsolete once the onDestroy(boolean) is
     * called
     *
     * @param editor The rich text editor to unregister.
     */
    public void unregisterEditor(RTEditText editor) {
        mEditors.remove(editor.getId());
        editor.unregister();

        updateToolbarVisibility();
    }

    /**
     * Register a toolbar.
     * <p>
     * Only after doing that can it be used in conjunction with a rich text editor.
     * Must be called from the ui thread.
     *
     * @param toolbarContainer The ViewGroup containing the toolbar.
     *                         This container is used to show/hide the toolbar if needed (e.g. if the RTEditText field loses/gains focus).
     *                         We can't use the toolbar itself because there could be multiple and they could be embedded in a complex layout hierarchy.
     * @param toolbar          The toolbar to register.
     */
    public void registerToolbar(ViewGroup toolbarContainer, RTToolbar toolbar) {
        mToolbars.put(toolbar.getId(), toolbar);
        toolbar.setToolbarListener(this);
        toolbar.setToolbarContainer(toolbarContainer);
        updateToolbarVisibility();
        Log.v("registerToolbar: ","toolbar_registered" );
    }

    /**
     * Unregister a toolbar.
     * <p>
     * This method may be called before the component is destroyed to
     * stop any interaction with the toolbar. Not doing so may result
     * in (asynchronous) calls coming through when the Activity/Fragment
     * is already stopping its operation.
     * <p>
     * Must be called from the ui thread.
     * <p>
     * Important: calling this method is obsolete once the
     * onDestroy(boolean) is called
     *
     * @param toolbar The toolbar to unregister.
     */
    public void unregisterToolbar(RTToolbar toolbar) {
        mToolbars.remove(toolbar.getId());
        toolbar.removeToolbarListener();
        updateToolbarVisibility();
    }

    /**
     * Set the auto show/hide toolbar mode.
     */
    public void setToolbarVisibility(ToolbarVisibility toolbarVisibility) {
        if (mToolbarVisibility != toolbarVisibility) {
            mToolbarVisibility = toolbarVisibility;
            updateToolbarVisibility();
        }
    }

    private void updateToolbarVisibility() {
        boolean showToolbars = mToolbarVisibility == SHOW;

        if (mToolbarVisibility == AUTOMATIC) {
            RTEditText editor = getActiveEditor();
            showToolbars = editor != null && editor.usesRTFormatting();
        }

        for (RTToolbar toolbar : mToolbars.values()) {
            setToolbarVisibility(toolbar, showToolbars);
        }
    }

    private void setToolbarVisibility(final RTToolbar toolbar, final boolean visible) {
        mToolbarIsVisible = visible;

        final ViewGroup toolbarContainer = toolbar.getToolbarContainer();
        int visibility = View.VISIBLE;
        synchronized (toolbarContainer) {
            visibility = toolbarContainer.getVisibility();
        }

        // only change visibility if we actually have to
        if ((visibility == View.GONE && visible) || (visibility == View.VISIBLE && !visible)) {

            AlphaAnimation fadeAnimation = visible ? new AlphaAnimation(0.0f, 1.0f) : new AlphaAnimation(1.0f, 0.0f);
            fadeAnimation.setDuration(400);
            fadeAnimation.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    synchronized (toolbarContainer) {
                        toolbarContainer.setVisibility(mToolbarIsVisible ? View.VISIBLE : View.GONE);
                    }
                }
            });

            toolbarContainer.startAnimation(fadeAnimation);
        } else {
            toolbarContainer.clearAnimation();
        }
    }


    @Override
    /* @inheritDoc */
    public void onCreateLink() {
        RTEditText editor = getActiveEditor();

        if (editor != null) {
            String url = null;
            String linkText = null;

            List<RTSpan<String>> links = Effects.LINK.getSpans(editor.getText(), new Selection(editor), SpanCollectMode.EXACT);
            if (links.isEmpty()) {
                // default values if no link is found at selection
                linkText = editor.getSelectedText();
                try {
                    // if this succeeds we have a valid URL and will use it for the link
                    new URL(linkText);
                    url = linkText;
                } catch (MalformedURLException ignore) {
                }
                mLinkSelection = editor.getSelection();
            } else {
                // values if a link already exists
                RTSpan<String> linkSpan = links.get(0);
                url = linkSpan.getValue();
                linkText = getLinkText(editor, linkSpan);
            }

            mRTApi.openDialogFragment(ID_01_LINK_FRAGMENT, LinkFragment.newInstance(linkText, url));
        }
    }

    // ****************************************** RTToolbarListener *******************************************

    @Override
    /* @inheritDoc */
    public <V, C extends RTSpan<V>> void onEffectSelected(Effect<V, C> effect, V value) {
        RTEditText editor = getActiveEditor();
        if (editor != null) {
            editor.applyEffect(effect, value);
        }
    }


    @Override
    /* @inheritDoc */
    public void onClearFormatting() {
        RTEditText editor = getActiveEditor();
        if (editor != null) {
            int selStartBefore = editor.getSelectionStart();
            int selEndBefore = editor.getSelectionEnd();
            Spannable oldSpannable = editor.cloneSpannable();
            for (Effect effect : Effects.FORMATTING_EFFECTS) {
                effect.clearFormattingInSelection(editor);
            }
            int selStartAfter = editor.getSelectionStart();
            int selEndAfter = editor.getSelectionEnd();
            Spannable newSpannable = editor.cloneSpannable();
            mOPManager.executed(editor, new RTOperationManager.TextChangeOperation(oldSpannable, newSpannable,
                    selStartBefore, selEndBefore,
                    selStartAfter, selEndAfter));
        }
    }


    @Override
    /* @inheritDoc */
    public void onUndo() {
        RTEditText editor = getActiveEditor();
        if (editor != null) {
            mOPManager.undo(editor);
        }
    }

    @Override
    /* @inheritDoc */
    public void onRedo() {
        RTEditText editor = getActiveEditor();
        if (editor != null) {
            mOPManager.redo(editor);
        }
    }





    private RTEditText getActiveEditor() {
        for (RTEditText editor : mEditors.values()) {
            if (editor.hasFocus()) {
                return editor;
            }
        }
        return null;
    }

    // ****************************************** RTEditTextListener *******************************************



    @Override
    /* @inheritDoc */
    public void onFocusChanged(RTEditText editor, boolean focused) {
        if (editor.usesRTFormatting()) {
            synchronized (this) {
                // if a focus loss is pending then we cancel it
                if (mIsPendingFocusLoss) {
                    mCancelPendingFocusLoss = true;
                }
            }
            if (focused) {
                changeFocus();
            } else {
                mIsPendingFocusLoss = true;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        changeFocus();
                    }
                }, 10);
            }
        }
    }

    private void changeFocus() {
        synchronized (this) {
            if (!mCancelPendingFocusLoss) {
                updateToolbarVisibility();
            }
            mCancelPendingFocusLoss = false;
            mIsPendingFocusLoss = false;
        }
    }

    @Override
    /* @inheritDoc */
    public void onSelectionChanged(RTEditText editor, int start, int end) {
        if (editor == null) return;

        // default values
        boolean isBold = false;
        boolean isItalic = false;
        boolean isUnderLine = false;
        boolean isStrikethrough = false;
        boolean isSuperscript = false;
        boolean isSubscript = false;
        boolean isBullet = false;
        boolean isNumber = false;
        List<Alignment> alignments = null;
        List<Integer> sizes = null;
        List<Integer> fontColors = null;
        List<Integer> bgColors = null;

        // check if effect exists in selection
        for (com.deltastream.example.edittextcontroller.effects.Effect effect : Effects.ALL_EFFECTS) {
            if (effect.existsInSelection(editor)) {
                if (effect instanceof BoldEffect) {
                    isBold = true;
                } else if (effect instanceof ItalicEffect) {
                    isItalic = true;
                } else if (effect instanceof UnderlineEffect) {
                    isUnderLine = true;
                } else if (effect instanceof StrikethroughEffect) {
                    isStrikethrough = true;
                } else if (effect instanceof SuperscriptEffect) {
                    isSuperscript = true;
                } else if (effect instanceof SubscriptEffect) {
                    isSubscript = true;
                } else if (effect instanceof BulletEffect) {
                    isBullet = true;
                } else if (effect instanceof NumberEffect) {
                    isNumber = true;
                } else if (effect instanceof AlignmentEffect) {
                    alignments = Effects.ALIGNMENT.valuesInSelection(editor);
                } else if (effect instanceof AbsoluteSizeEffect) {
                    sizes = Effects.FONTSIZE.valuesInSelection(editor);
                }
                else if (effect instanceof ForegroundColorEffect) {
                    fontColors = Effects.FONTCOLOR.valuesInSelection(editor);
                }
            }
        }

        // update toolbar(s)
        for (RTToolbar toolbar : mToolbars.values()) {
            toolbar.setBold(isBold);
            toolbar.setItalic(isItalic);
            toolbar.setUnderline(isUnderLine);
            toolbar.setStrikethrough(isStrikethrough);
            toolbar.setBullet(isBullet);
            toolbar.setNumber(isNumber);

            // alignment (left, center, right)

            // alignment (left, center, right)
            if (alignments != null && alignments.size() == 1) {
                toolbar.setAlignment(alignments.get(0));
            } else {
                boolean isRTL = Helper.isRTL(editor.getText(), start, end);
                toolbar.setAlignment(isRTL ? Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL);
            }
            if (fontColors != null) {
                toolbar.setFontColor(Color.parseColor("#c8c8c8"));
            } else {

            }
        }

    }

    @Override
    /* @inheritDoc */
    public void onTextChanged(RTEditText editor, Spannable before, Spannable after,
                              int selStartBefore, int selEndBefore, int selStartAfter, int selEndAfter) {





        RTOperationManager.TextChangeOperation op = new RTOperationManager.TextChangeOperation(before, after,
                selStartBefore, selEndBefore,
                selStartAfter, selEndAfter);
        mOPManager.executed(editor, op);
    }


    private String getLinkText(RTEditText editor, RTSpan<String> span) {
        Spannable text = editor.getText();
        final int spanStart = text.getSpanStart(span);
        final int spanEnd = text.getSpanEnd(span);
        String linkText = null;
        if (spanStart >= 0 && spanEnd >= 0 && spanEnd <= text.length()) {
            linkText = text.subSequence(spanStart, spanEnd).toString();
            mLinkSelection = new Selection(spanStart, spanEnd);
        } else {
            mLinkSelection = editor.getSelection();
        }
        return linkText;
    }


    /**
     * LinkFragment has closed -> process the result.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LinkFragment.LinkEvent event) {
        final String fragmentTag = event.getFragmentTag();
        mRTApi.removeFragment(fragmentTag);

        if (!event.wasCancelled() && ID_01_LINK_FRAGMENT.equals(fragmentTag)) {

            RTEditText editor = getActiveEditor();
            if (editor != null) {

                LinkFragment.Link link = event.getLink();
                String url = null;
                if (link != null && link.isValid()) {

                    // the mLinkSelection.end() <= editor.length() check is necessary since
                    // the editor text can change when the link fragment is open
                    Selection selection = mLinkSelection != null && mLinkSelection.end() <= editor.length() ? mLinkSelection : new Selection(editor);

                    String linkText = link.getLinkText();

                    // if no text is selected this inserts the entered link text
                    // if text is selected we replace it by the link text
                    Editable str = editor.getText();
                    str.replace(selection.start(), selection.end(), linkText);
                    editor.setSelection(selection.start(), selection.start() + linkText.length());

                    url = link.getUrl();

                }
                editor.applyEffect(Effects.LINK, url);    // if url == null -> remove the link

            }

        }
    }

    @Override
    /* @inheritDoc */
    public void onClick(RTEditText editor, LinkSpan span) {
        if (editor != null) {
            String linkText = getLinkText(editor, span);
            mRTApi.openDialogFragment(ID_01_LINK_FRAGMENT, LinkFragment.newInstance(linkText, span.getURL()));
        }
    }

    @Override
    public void onRichTextEditingChanged(RTEditText editor, boolean useRichText) {
        updateToolbarVisibility();
    }


    @Override
    public void onMentionSelected() {
        Log.e( "onMentionSelected: "," mention" );
        //onEffectSelected(Effects.FONTCOLOR, Color.parseColor("#fa2d65"));
    }

    @Override
    public void onHashTagSelected() {

//        onEffectSelected(Effects.FONTCOLOR,Color.parseColor("#465ED3"));
          //getActiveEditor().append("@",getActiveEditor().getSelectionStart(),getActiveEditor().getSelectionStart()+1);

    }

    @Override
    public void onStopMentioning() {

        //onEffectSelected(Effects.FONTCOLOR,Color.parseColor("#000000"));

        Log.e( "onStopMentionSelected: "," mention stop" );
    }

    @Override
    public void onStopHashTags() {

        Log.e( "onStopHashtagSelected: "," hashtag" );
        //onEffectSelected(Effects.FONTCOLOR,Color.parseColor("#000000"));
    }


}