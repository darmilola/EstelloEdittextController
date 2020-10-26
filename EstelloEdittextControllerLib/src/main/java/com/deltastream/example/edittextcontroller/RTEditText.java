/*
 * Copyright 2015 Emanuel Moecklin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.deltastream.example.edittextcontroller;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;

import android.net.rtp.RtpStream;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ParagraphStyle;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.deltastream.example.edittextcontroller.api.RTApi;
import com.deltastream.example.edittextcontroller.api.format.RTEditable;
import com.deltastream.example.edittextcontroller.api.format.RTFormat;
import com.deltastream.example.edittextcontroller.api.format.RTHtml;
import com.deltastream.example.edittextcontroller.api.format.RTPlainText;
import com.deltastream.example.edittextcontroller.api.format.RTText;
import com.deltastream.example.edittextcontroller.effects.Effect;
import com.deltastream.example.edittextcontroller.effects.Effects;
import com.deltastream.example.edittextcontroller.spans.BulletSpan;
import com.deltastream.example.edittextcontroller.spans.LinkSpan;

import com.deltastream.example.edittextcontroller.spans.NumberSpan;
import com.deltastream.example.edittextcontroller.spans.RTSpan;
import com.deltastream.example.edittextcontroller.utils.Paragraph;
import com.deltastream.example.edittextcontroller.utils.RTLayout;
import com.deltastream.example.edittextcontroller.utils.Selection;


import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
import androidx.core.util.Supplier;
/**

 * The actual rich text editor (extending android.widget.EditText).
 */
public class RTEditText extends AppCompatEditText implements TextWatcher, SpanWatcher, LinkSpan.LinkSpanListener,SocialView{


    // don't allow any formatting in text mode
    private boolean mUseRTFormatting = true;
    private static final int FLAG_HASHTAG = 1;
    private static final int FLAG_MENTION = 2;

    // for performance reasons we compute a new layout only if the text has changed
    private boolean mLayoutChanged;
    private RTLayout mRTLayout;    // don't call this mLayout because TextView has a mLayout too (no shadowing as both are private but still...)

    // while onSaveInstanceState() is running, don't modify any spans
    private boolean mIsSaving;

    /// while selection is changing don't apply any effects
    private boolean mIsSelectionChanging = false;

    // text has changed
    private boolean mTextChanged;

    // this indicates whether text is selected or not -> ignore window focus changes (by spinners)
    private boolean mTextSelected;

    private RTEditTextListener mListener;


    // used to check if selection has changed
    private int mOldSelStart = -1;
    private int mOldSelEnd = -1;
    // we don't want to call Effects.cleanupParagraphs() if the paragraphs are already up to date
    private boolean mParagraphsAreUp2Date;
    // while Effects.cleanupParagraphs() is called, we ignore changes that would alter mParagraphsAreUp2Date
    private boolean mIgnoreParagraphChanges;

    /* Used for the undo / redo functions */

    // if True then text changes are not registered for undo/redo
    // we need this during the actual undo/redo operation (or an undo would create a change event itself)
    private boolean mIgnoreTextChanges;

    // Keep track of ordered list spans.
    // If one of them is selected, then append zero with char after new line.
    // This fixes the bug when bullet/number span is not applied to empty string
    private boolean mIsBulletSpanSelected;
    private boolean mIsNumberSpanSelected;
    // The length of text before new char is added
    private int mPreviousTextLength;

    private int mSelStartBefore;        // selection start before text changed
    private int mSelEndBefore;          // selection end before text changed
    private String mOldText;            // old text before it changed
    private String mNewText;            // new text after it changed (needed in afterTextChanged to see if the text has changed)
    private Spannable mOldSpannable;    // undo/redo
    private boolean hashtagEditing = false,mentionEditing = false;

    @Nullable
    private Pattern hashtagPattern;
    @Nullable private Pattern mentionPattern;
    @Nullable private Pattern hyperlinkPattern;
    private int flags;
    @NonNull
    private ColorStateList hashtagColors;
    @NonNull private ColorStateList mentionColors;
    @NonNull private ColorStateList hyperlinkColors;
    @Nullable private SocialView.OnClickListener hashtagClickListener;
    @Nullable private SocialView.OnClickListener mentionClickListener;
    @Nullable private SocialView.OnClickListener hyperlinkClickListener;
    @Nullable private OnChangedListener hashtagChangedListener;
    @Nullable private OnChangedListener mentionChangedListener;
    ForegroundColorSpan foregroundSpan;
    MentionHashTagListener mentionHashTagListener;
    int hashTagIsComing = 0;
    int mentionIsComing = 0;




    // ****************************************** Lifecycle Methods *******************************************


    public void setMentionHashTagListener(MentionHashTagListener mentionHashTagListener) {

        this.mentionHashTagListener = mentionHashTagListener;
    }

    public void setMentionChangedListener(@Nullable OnChangedListener mentionChangedListener) {
        this.mentionChangedListener = mentionChangedListener;
    }


    public RTEditText(Context context) {
        super(context);

    }

    public RTEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public RTEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        addTextChangedListener(this);
        // we need this or links won't be clickable
        setMovementMethod(RTEditorMovementMethod.getInstance());
        //setText(getText(RTFormat.PLAIN_TEXT), TextView.BufferType.SPANNABLE);
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ctr);
        flags = a.getInteger(R.styleable.ctr_socialFlag, FLAG_HASHTAG | FLAG_MENTION);
        hashtagColors = a.getColorStateList(R.styleable.ctr_hashtagColor);
        mentionColors = a.getColorStateList(R.styleable.ctr_mentionColor);
        foregroundSpan = new ForegroundColorSpan(Color.parseColor("#fa2d65"));

    }

    /**
     * @param isSaved True if the text is saved, False if it's dismissed
     */
    void onDestroy(boolean isSaved) {
        // make sure all obsolete MediaSpan files are removed from the file system:
        // - when saving the text delete the MediaSpan if it was deleted
        // - when dismissing the text delete the MediaSpan if it was deleted and not saved before

        // collect all media the editor contains currently

        Spannable text = getText();


        // now delete all those that aren't needed any longer

    }


    void register(RTEditTextListener listener) {
        mListener = listener;

    }

    /**
     * Usually called from the RTManager.onDestroy() method
     */
    void unregister() {
        mListener = null;

    }

    /**
     * Return all paragraphs as as array of selection objects
     */
    public ArrayList<Paragraph> getParagraphs() {
        return getRTLayout().getParagraphs();
    }

    /**
     * Find the start and end of the paragraph(s) encompassing the current selection.
     * A paragraph spans from one \n (exclusive) to the next one (inclusive)
     */
    public Selection getParagraphsInSelection() {
        RTLayout layout = getRTLayout();

        Selection selection = new Selection(this);
        int firstLine = layout.getLineForOffset(selection.start());
        int end = selection.isEmpty() ? selection.end() : selection.end() - 1;
        int lastLine = layout.getLineForOffset(end);

        return new Selection(layout.getLineStart(firstLine), layout.getLineEnd(lastLine));
    }

    private RTLayout getRTLayout() {
        synchronized (this) {
            if (mRTLayout == null || mLayoutChanged) {
                mRTLayout = new RTLayout(getText());
                mLayoutChanged = false;
            }
        }
        return mRTLayout;
    }

    /**
     * This method returns the Selection which makes sure that selection start is <= selection end.
     * Note: getSelectionStart()/getSelectionEnd() refer to the order in which text was selected.
     */
    Selection getSelection() {
        int selStart = getSelectionStart();
        int selEnd = getSelectionEnd();
        return new Selection(selStart, selEnd);
    }

    /**
     * @return the selected text (needed when creating links)
     */
    String getSelectedText() {
        Spannable text = getText();
        Selection sel = getSelection();
        if (sel.start() >= 0 && sel.end() >= 0 && sel.end() <= text.length()) {
            return text.subSequence(sel.start(), sel.end()).toString();
        }
        return null;
    }

    public Spannable cloneSpannable() {
        CharSequence text = super.getText();
        return new ClonedSpannableString(text != null ? text : "");
    }

    // ****************************************** Set/Get Text Methods *******************************************

    /**
     * Sets the edit mode to plain or rich text. The text will be converted
     * automatically to rich/plain text if autoConvert is True.
     *
     * @param useRTFormatting True if the edit mode should be rich text, False if the edit
     *                        mode should be plain text
     * @param autoConvert     Automatically convert the content to plain or rich text if
     *                        this is True
     */
    public void setRichTextEditing(boolean useRTFormatting, boolean autoConvert) {


        if (useRTFormatting != mUseRTFormatting) {
            mUseRTFormatting = useRTFormatting;

            if (autoConvert) {
                RTFormat targetFormat = useRTFormatting ? RTFormat.PLAIN_TEXT : RTFormat.HTML;
                setText(getRichText(targetFormat));
            }

            if (mListener != null) {
                mListener.onRichTextEditingChanged(this, mUseRTFormatting);
            }
        }
    }

    /**
     * Sets the edit mode to plain or rich text and updates the content at the
     * same time. The caller needs to make sure the content matches the correct
     * format (if you pass in html code as plain text the editor will show the
     * html code).
     *
     * @param useRTFormatting True if the edit mode should be rich text, False if the edit
     *                        mode should be plain text
     * @param content         The new content
     */
    public void setRichTextEditing(boolean useRTFormatting, String content) {


        if (useRTFormatting != mUseRTFormatting) {
            mUseRTFormatting = useRTFormatting;

            if (mListener != null) {
                mListener.onRichTextEditingChanged(this, mUseRTFormatting);
            }
        }

        RTText rtText = useRTFormatting ?
                new RTHtml(RTFormat.HTML, content) :
                new RTPlainText(content);

        setText(rtText);
    }

    /**
     * Set the text for this editor.
     * <p>
     * It will convert the text from rich text to plain text if the editor's
     * mode is set to use plain text. or to a spanned text (only supported
     * formatting) if the editor's mode is set to use rich text
     * <p>
     * We need to prevent onSelectionChanged() to do anything as long as
     * setText() hasn't finished because the Layout doesn't seem to update
     * before setText has finished but onSelectionChanged will still be called
     * during setText and will receive the out-dated Layout which doesn't allow
     * us to apply styles and such.
     */
    public void setText(RTText rtText) {


        if (rtText.getFormat() instanceof RTFormat.Html) {
            if (mUseRTFormatting) {
                RTText rtSpanned = rtText.convertTo(RTFormat.SPANNED);

                super.setText(rtSpanned.getText(), BufferType.EDITABLE);
                addSpanWatcher();

                // collect all current media
                Spannable text = getText();


                Effects.cleanupParagraphs(this);
            }

            else {
                RTText rtPlainText = rtText.convertTo(RTFormat.PLAIN_TEXT);
                super.setText(rtPlainText.getText());
            }

        } else if (rtText.getFormat() instanceof RTFormat.PlainText) {
            CharSequence text = rtText.getText();
            super.setText(text == null ? "" : text.toString());
        }

        onSelectionChanged(0, 0);
    }

    public boolean usesRTFormatting() {
        return mUseRTFormatting;
    }

    /**
     * Returns the content of this editor as a String. The caller is responsible
     * to call only formats that are supported by RTEditable (which is the rich
     * text editor's format and always the source format).
     *
     * @param format The RTFormat the text should be converted to.
     * @throws UnsupportedOperationException if the target format isn't supported.
     */
    public String getText(RTFormat format) {
        return getRichText(format).getText().toString();
    }

    /**
     * Same as "String getText(RTFormat format)" but this method returns the
     * RTText instead of just the actual text.
     */
    public RTText getRichText(RTFormat format) {


        RTEditable rtEditable = new RTEditable(this);
        return rtEditable.convertTo(format);
    }




    // ****************************************** TextWatcher / SpanWatcher *******************************************

    public boolean hasChanged() {
        return mTextChanged;
    }

    public void resetHasChanged() {
        mTextChanged = false;
        setParagraphsAreUp2Date(false);
    }

    /**
     * Ignore changes that would trigger a RTEditTextListener.onTextChanged()
     * method call. We need this during the actual undo/redo operation (or an
     * undo would create a change event itself).
     */
    synchronized void ignoreTextChanges() {
        mIgnoreTextChanges = true;
    }

    /**
     * If changes happen call RTEditTextListener.onTextChanged().
     * This is needed for the undo/redo functionality.
     */
    synchronized void registerTextChanges() {
        mIgnoreTextChanges = false;
    }

    @Override
    /* TextWatcher */
    public synchronized void beforeTextChanged(CharSequence s, int start, int count, int after) {

        // we use a String to get a static copy of the CharSequence (the CharSequence changes when the text changes...)

        Log.e("beforeTextChanged:   ",s.toString() );
        String oldText = mOldText == null ? "" : mOldText;
        if (!mIgnoreTextChanges && !s.toString().equals(oldText)) {
            mSelStartBefore = getSelectionStart();
            mSelEndBefore = getSelectionEnd();
            mOldText = s.toString();
            mNewText = mOldText;
            mOldSpannable = cloneSpannable();
            //mListener.onTextChanged(this, mOldSpannable, prevOldSpannable, mSelStartBefore, mSelEndBefore, getSelectionStart(), getSelectionEnd());

        }
        mLayoutChanged = true;
        mPreviousTextLength = s.length();
        //addSpanWatcher();
    }






    @Override
    /* TextWatcher */
    public synchronized void onTextChanged(CharSequence s, int start, int before, int count) {

        if (s.length() == 0) {
            return;
        }
        if(start < s.length()){
           int index = start + count - 1;
            Spannable spannable = getText();
           if(index < 0) return;
            switch (s.charAt(index)) {
                case '#':
                    hashtagEditing = true;
                    mentionEditing = false;

                      if((index > 0) && !(s.charAt(index - 1) == ' ')){
                         hashtagEditing = false;
                         hashTagIsComing = 0;
                          if(mentionHashTagListener != null){

                              mentionHashTagListener.onStopHashTags();
                          }
                      }
                      else {

                          authHashTagColor(spannable, s.toString().substring(start), start, start + count);
                          hashTagIsComing++;
                      }

                    break;
                case '@':
                    hashtagEditing = false;
                    mentionEditing = true;


                    if((index > 0) && !(s.charAt(index - 1) == ' ')){

                        mentionEditing = false;
                        mentionIsComing = 0;

                        if(mentionHashTagListener != null){
                            mentionHashTagListener.onStopMentioning();
                        }
                    }
                    else {

                        authMentionTagColor(spannable, s.toString().substring(start), start, start + count);
                        mentionIsComing++;
                    }

                    break;
                default:
                    if (!Character.isLetterOrDigit(s.charAt(index))) {

                        hashtagEditing = false;
                        mentionEditing = false;
                        hashTagIsComing = 0;
                        mentionIsComing = 0;

                        if(mentionHashTagListener != null){
                            mentionHashTagListener.onStopHashTags();
                            mentionHashTagListener.onStopMentioning();
                        }


                    }
                    else if (mentionHashTagListener != null && hashtagEditing) {
                        mentionHashTagListener.onHashTagSelected(s.subSequence(
                                indexOfPreviousNonLetterDigit(s, 0, start) + 1, start + count
                        ));
                    } else if (mentionHashTagListener != null && mentionEditing) {
                        mentionHashTagListener.onMentionSelected(s.subSequence(
                                indexOfPreviousNonLetterDigit(s, 0, start) + 1, start + count
                        ));
                    }
                    if (hashTagIsComing != 0) {

                        authHashTagColor(spannable, s.toString().substring(start), start, start + count);
                        hashTagIsComing++;
                    }
                    if (mentionIsComing != 0) {

                        authMentionTagColor(spannable, s.toString().substring(start), start, start + count);
                        mentionIsComing++;
                    }

                    break;

            }

            }
        mLayoutChanged = true;
        }



    @Override
    /* TextWatcher */
    public synchronized void afterTextChanged(Editable s) {


        //getText().removeSpan(foregroundSpan);
        if (mIsBulletSpanSelected || mIsNumberSpanSelected) {
            boolean mBackSpace = mPreviousTextLength >= s.length();
            if (!mBackSpace && s.toString().endsWith("\n")) {
                // append zero width character
                this.append("\u200B");
            }
        }



        String theText = s.toString();
        String newText = mNewText == null ? "" : mNewText;
        if (mListener != null && !mIgnoreTextChanges && !newText.equals(theText)) {
            Spannable newSpannable = cloneSpannable();
            mListener.onTextChanged(this, mOldSpannable, newSpannable, mSelStartBefore, mSelEndBefore, getSelectionStart(), getSelectionEnd());
            mNewText = theText;
        }
        mLayoutChanged = true;
        mTextChanged = true;
        setParagraphsAreUp2Date(false);
        addSpanWatcher();

    }

    private void authHashTagColor(Spannable spannable,String s, int start, int end){

        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext(),R.color.blue)),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

    }

    private void authMentionTagColor(Spannable spannable,String s, int start, int end){

        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext(),R.color.pinkypinky)),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

    }
    @Override
    /* SpanWatcher */
    public void onSpanAdded(Spannable text, Object what, int start, int end) {
        mTextChanged = true;
        // we need to keep track of ordered list spans
        if (what instanceof BulletSpan) {
            mIsBulletSpanSelected = true;
            // if text was empty then append zero width char
            // in order for the bullet to be shown when the span is selected
            if (text.toString().isEmpty()) {
                this.append("\u200B");
            }
        } else if (what instanceof NumberSpan) {
            mIsNumberSpanSelected = true;
            // if text was empty then append zero width char
            // in order for the number to be shown when the span is selected
            if (text.toString().isEmpty()) {
                this.append("\u200B");
            }
        }

        if (what instanceof RTSpan && what instanceof ParagraphStyle) {
            setParagraphsAreUp2Date(false);
        }
    }

    @Override
    /* SpanWatcher */
    public void onSpanChanged(Spannable text, Object what, int ostart, int oend, int nstart, int nend) {
        mTextChanged = true;
        if (what instanceof RTSpan && what instanceof ParagraphStyle) {
            setParagraphsAreUp2Date(false);
        }
    }

    @Override
    /* SpanWatcher */
    public void onSpanRemoved(Spannable text, Object what, int start, int end) {
        mTextChanged = true;
        // we need to keep track of ordered list spans
        if (what instanceof BulletSpan) {
            mIsBulletSpanSelected = false;
        } else if (what instanceof NumberSpan) {
            mIsNumberSpanSelected = false;
        }

        if (what instanceof RTSpan && what instanceof ParagraphStyle) {
            setParagraphsAreUp2Date(false);
        }
    }

    /**
     * Add a SpanWatcher for the Changeable implementation
     */
    private void addSpanWatcher() {
        Spannable spannable = getText();
        if (spannable.getSpans(0, spannable.length(), getClass()) != null) {
            spannable.setSpan(this, 0, spannable.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }

    synchronized private void setParagraphsAreUp2Date(boolean value) {
        if (! mIgnoreParagraphChanges) {
            mParagraphsAreUp2Date = value;
        }
    }

    // ****************************************** Listener Methods *******************************************

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        // if text is selected we ignore a loss of focus to prevent Android from terminating
        // text selection when one of the spinners opens (text size, color, bg color)
        if (!mUseRTFormatting || hasWindowFocus || !mTextSelected) {
            super.onWindowFocusChanged(hasWindowFocus);
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        if (mUseRTFormatting && mListener != null) {
            mListener.onFocusChanged(this, focused);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        mIsSaving = true;

        Parcelable superState = super.onSaveInstanceState();
        String content = getText(mUseRTFormatting ? RTFormat.HTML : RTFormat.PLAIN_TEXT);
        SavedState savedState = new SavedState(superState, mUseRTFormatting, content);

        mIsSaving = false;
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(state instanceof SavedState) {
            SavedState savedState = (SavedState)state;
            super.onRestoreInstanceState(savedState.getSuperState());
            setRichTextEditing(savedState.useRTFormatting(), savedState.getContent());
        }
        else {
            super.onRestoreInstanceState(state);
        }


    }

    private static class SavedState extends BaseSavedState {
        private String mContent;
        private boolean mUseRTFormatting;

        SavedState(Parcelable superState, boolean useRTFormatting, String content) {
            super(superState);

            mUseRTFormatting = useRTFormatting;
            mContent = content;
        }

        private String getContent() {
            return mContent;
        }

        private boolean useRTFormatting() {
            return mUseRTFormatting;
        }

        private SavedState(Parcel in) {
            super(in);

            mUseRTFormatting = in.readInt() == 1;
            mContent = in.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);

            out.writeInt(mUseRTFormatting ? 1 : 0);
            out.writeString(mContent);
        }

        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    @Override
    protected void onSelectionChanged(int start, int end) {


        if (mOldSelStart != start || mOldSelEnd != end) {
            mOldSelStart = start;
            mOldSelEnd = end;

            mTextSelected = (end > start);

            super.onSelectionChanged(start, end);

            if (mUseRTFormatting) {

                if (!mIsSaving && !mParagraphsAreUp2Date) {
                    mIgnoreParagraphChanges = true;
                    Effects.cleanupParagraphs(this);
                    mIgnoreParagraphChanges = false;
                    setParagraphsAreUp2Date(true);
                }

                if (mListener != null) {

                    mIsSelectionChanging = true;
                    mListener.onSelectionChanged(this, start, end);
                    mIsSelectionChanging = false;

                }

            }
        }
    }

    /**
     * Call this to have an effect applied to the current selection.
     * You get the Effect object via the static data members (e.g., RTEditText.BOLD).
     * The value for most effects is a Boolean, indicating whether to add or remove the effect.
     */
    public <V extends Object, C extends RTSpan<V>> void applyEffect(Effect<V, C> effect, V value) {
        if (mUseRTFormatting && !mIsSelectionChanging && !mIsSaving) {
            Spannable oldSpannable = mIgnoreTextChanges ? null : cloneSpannable();

            effect.applyToSelection(this, value);

            synchronized (this) {
                if (mListener != null && !mIgnoreTextChanges) {
                    Spannable newSpannable = cloneSpannable();
                    mListener.onTextChanged(this, oldSpannable, newSpannable, getSelectionStart(), getSelectionEnd(),
                            getSelectionStart(), getSelectionEnd());
                }
                mLayoutChanged = true;
            }
        }
    }

    @Override
    /* LinkSpanListener */
    public void onClick(LinkSpan linkSpan) {
        if (mUseRTFormatting && mListener != null) {
           mListener.onClick(this, linkSpan);
        }
    }

    /**
     * Returns regex that are responsible for finding <b>hashtags</b>.
     * By default, the pattern are {@code #(\w+)}.
     */
    @NonNull
    @Override
    public Pattern getHashtagPattern() {

        return hashtagPattern != null ? hashtagPattern : Pattern.compile("#(\\w+)");
    }

    @NonNull
    @Override
    public Pattern getMentionPattern() {
        return mentionPattern != null ? mentionPattern : Pattern.compile("@(\\w+)");
    }



    @Override
    public void setHashtagPattern(@Nullable Pattern pattern) {
        if (hashtagPattern != pattern) {
            hashtagPattern = pattern;

        }
    }

    @Override
    public void setMentionPattern(@Nullable Pattern pattern) {
        if (mentionPattern != null) {
            mentionPattern = pattern;

        }
    }



    @Override
    public boolean isHashtagEnabled() {
        return (flags | FLAG_HASHTAG) == flags;
    }

    @Override
    public boolean isMentionEnabled() {
        return (flags | FLAG_MENTION) == flags;
    }



    @Override
    public void setHashtagEnabled(boolean enabled) {
        if (enabled != isHashtagEnabled()) {
            flags = enabled ? flags | FLAG_HASHTAG : flags & (~FLAG_HASHTAG);

        }
    }

    @Override
    public void setMentionEnabled(boolean enabled) {
        if (enabled != isMentionEnabled()) {
            flags = enabled ? flags | FLAG_MENTION : flags & (~FLAG_MENTION);

        }
    }



    @NonNull
    @Override
    public ColorStateList getHashtagColors() {
        return hashtagColors;
    }

    @NonNull
    @Override
    public ColorStateList getMentionColors() {
        return mentionColors;
    }



    @Override
    public void setHashtagColors(@NonNull ColorStateList colors) {
        hashtagColors = colors;

    }

    @Override
    public void setMentionColors(@NonNull ColorStateList colors) {
        mentionColors = colors;

    }


    @Override
    public int getHashtagColor() {
        return getHashtagColors().getDefaultColor();
    }

    @Override
    public int getMentionColor() {
        return getMentionColors().getDefaultColor();
    }



    @Override
    public void setHashtagColor(int color) {
        setHashtagColors(ColorStateList.valueOf(color));
    }

    @Override
    public void setMentionColor(int color) {
        setMentionColors(ColorStateList.valueOf(color));
    }

    @Override
    public void setOnHashtagClickListener(@Nullable SocialView.OnClickListener listener) {
        //ensureMovementMethod(listener);
        hashtagClickListener = listener;

    }

    @Override
    public void setOnMentionClickListener(@Nullable SocialView.OnClickListener listener) {
        //ensureMovementMethod(listener);
        mentionClickListener = listener;

    }



    @Override
    public void setHashtagTextChangedListener(@Nullable OnChangedListener listener) {
        hashtagChangedListener = listener;
    }

    @Override
    public void setMentionTextChangedListener(@Nullable OnChangedListener listener) {
        mentionChangedListener = listener;
    }

    @NonNull
    @Override
    public List<String> getHashtags() {
        return listOf(getText(RTFormat.PLAIN_TEXT), getHashtagPattern());
    }

    @NonNull
    @Override
    public List<String> getMentions() {
        return listOf(getText(RTFormat.SPANNED), getMentionPattern());
    }




    private static int indexOfNextNonLetterDigit(CharSequence text, int start) {
        for (int i = start + 1; i < text.length(); i++) {
            if (!Character.isLetterOrDigit(text.charAt(i))) {
                return i;
            }
        }
        return text.length();
    }

    private static int indexOfPreviousNonLetterDigit(CharSequence text, int start, int end) {
        for (int i = end; i > start; i--) {
            if (!Character.isLetterOrDigit(text.charAt(i))) {
                return i;
            }
        }
        return start;
    }

    private static void spanAll(Spannable spannable, Pattern pattern, Supplier<CharacterStyle> styleSupplier) {
        final Matcher matcher = pattern.matcher(spannable);
        while (matcher.find()) {
            final int start = matcher.start();
            final int end = matcher.end();
            final Object span = styleSupplier.get();
            spannable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (span instanceof SocialClickableSpan) {
                ((SocialClickableSpan) span).text = spannable.subSequence(start, end);
            }
        }
    }

    private static List<String> listOf(CharSequence text, Pattern pattern) {
        final List<String> list = new ArrayList<>();
        final Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            list.add(matcher.group(0));
        }
        return list;
    }

    private static class SocialClickableSpan extends ClickableSpan {
        private final SocialView.OnClickListener listener;
        private final int color;
        private final boolean isHyperlink;
        private CharSequence text;

        private SocialClickableSpan(SocialView.OnClickListener listener, ColorStateList colors, boolean isHyperlink) {
            this.listener = listener;
            this.color = colors.getDefaultColor();
            this.isHyperlink = isHyperlink;
        }

        @Override
        public void onClick(@NonNull View widget) {
            if (!(widget instanceof SocialView)) {
                throw new IllegalStateException("Clicked widget is not an instance of SocialView.");
            }
            listener.onClick((SocialView) widget, !isHyperlink ? text.subSequence(1, text.length()) : text);
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            ds.setColor(color);
            ds.setUnderlineText(isHyperlink);
        }
    }

    /**
     * Default {@link CharacterStyle} for <b>hyperlinks</b>.
     */
    private static class SocialURLSpan extends URLSpan {
        private final int color;

        private SocialURLSpan(CharSequence url, ColorStateList colors) {
            super(url.toString());
            this.color = colors.getDefaultColor();
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            ds.setColor(color);
            ds.setUnderlineText(true);
        }
    }


}