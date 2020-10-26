package com.deltastream.example.edittextcontroller;

import android.content.res.ColorStateList;
import android.widget.TextView;


import java.util.List;
import java.util.regex.Pattern;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.PatternsCompat;


public interface SocialView {

    /**
     * Returns regex that are responsible for finding <b>hashtags</b>.
     * By default, the pattern are {@code #(\w+)}.
     */
    @NonNull
    Pattern getHashtagPattern();

    /**
     * Returns regex that are responsible for finding <b>mentions</b>.
     * By default, the pattern are {@code @(\w+)}.
     */
    @NonNull
    Pattern getMentionPattern();

    /**
     * Returns regex that are responsible for finding <b>hyperlinks</b>.
     * By default, the pattern are {@link PatternsCompat#WEB_URL}.
     */

    void setHashtagPattern(@Nullable Pattern pattern);

    /**
     * Modify regex that are responsible for finding <b>hashtags</b>.
     *
     * @param pattern custom regex. When null, default pattern will be used.
     */
    void setMentionPattern(@Nullable Pattern pattern);



    /**
     * Returns true if <b>hashtags</b> in this view are spanned.
     */
    boolean isHashtagEnabled();

    /**
     * Returns true if <b>mentions</b> in this view are spanned.
     */
    boolean isMentionEnabled();

    /**
     * Returns true if <b>hyperlinks</b> in this view are spanned.
     */

    void setHashtagEnabled(boolean enabled);

    /**
     * Determine whether this view should span <b>mentions</b>.
     *
     * @param enabled True when spanning should be enabled.
     */
    void setMentionEnabled(boolean enabled);



    /**
     * Returns color instance of <b>hashtags</b>, default is color accent of current app theme.
     * Will still return corresponding color even when {@link #isHashtagEnabled()} is false.
     */
    @NonNull
    ColorStateList getHashtagColors();

    /**
     * Returns color instance of <b>mentions</b>, default is color accent of current app theme.
     * Will still return corresponding color even when {@link #isMentionEnabled()} ()} is false.
     */
    @NonNull
    ColorStateList getMentionColors();



    /**
     * Sets <b>hashtags</b> color instance.
     *
     * @param colors Colors state list instance.
     */
    void setHashtagColors(@NonNull ColorStateList colors);

    /**
     * Sets <b>mentions</b> color instance.
     *
     * @param colors Colors state list instance.
     */
    void setMentionColors(@NonNull ColorStateList colors);




    /**
     * Returns color integer of <b>hashtags</b>.
     *
     * @see #getHashtagColors()
     */
    @ColorInt
    int getHashtagColor();

    /**
     * Returns color integer of <b>mentions</b>.
     *
     * @see #getMentionColors()
     */
    @ColorInt
    int getMentionColor();



    /**
     * Sets <b>hashtags</b> color integer.
     *
     * @param color Color integer.
     * @see #setHashtagColors(ColorStateList)
     */
    void setHashtagColor(@ColorInt int color);

    /**
     * Sets <b>mentions</b> color integer.
     *
     * @param color Color integer.
     * @see #setMentionColors(ColorStateList)
     */
    void setMentionColor(@ColorInt int color);



    /**
     * Registers a callback to be invoked when a <b>hashtag</b> is clicked.
     *
     * @param listener The callback that will run.
     */
    void setOnHashtagClickListener(@Nullable OnClickListener listener);

    /**
     * Registers a callback to be invoked when a <b>mention</b> is clicked.
     *
     * @param listener The callback that will run.
     */
    void setOnMentionClickListener(@Nullable OnClickListener listener);


    /**
     * Registers a text watcher to be invoked when a <b>hashtag</b> is modified.
     *
     * @param listener The callback that will run.
     */
    void setHashtagTextChangedListener(@Nullable OnChangedListener listener);

    /**
     * Registers a text watcher to be invoked when a <b>mention</b> is modified.
     *
     * @param listener The callback that will run.
     */
    void setMentionTextChangedListener(@Nullable OnChangedListener listener);

    /**
     * Returns list of all <b>hashtags</b> found in {@link TextView#getText()}.
     */
    @NonNull
    List<String> getHashtags();

    /**
     * Returns list of all <b>mentions</b> found in {@link TextView#getText()}.
     */
    @NonNull
    List<String> getMentions();

    /**
     * Interface definition for a callback to be invoked when a <b>hashtag</b>,
     * <b>mention</b>, or <b>hyperlink</b> is clicked.
     */
    interface OnClickListener {
        /**
         * Called when a text has been clicked.
         *
         * @param view The view that the texts belong to.
         * @param text The text that was clicked.
         */
        void onClick(@NonNull SocialView view, @NonNull CharSequence text);
    }

    /**
     * Interface definition for a callback to be invoked when a <b>hashtag</b>,
     * <b>mention</b>, or <b>hyperlink</b> is modified.
     */
    interface OnChangedListener {
        /**
         * Called when a text has been modified.
         *
         * @param view The view that the texts belong to.
         * @param text The text that was modified.
         */
        void onChanged(@NonNull SocialView view, @NonNull CharSequence text);
    }
}