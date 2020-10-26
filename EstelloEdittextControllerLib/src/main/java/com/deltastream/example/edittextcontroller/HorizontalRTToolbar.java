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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.deltastream.example.edittextcontroller.effects.Effect;
import com.deltastream.example.edittextcontroller.effects.Effects;

import java.util.concurrent.atomic.AtomicInteger;


public class HorizontalRTToolbar extends LinearLayout implements RTToolbar, View.OnClickListener {


    private static AtomicInteger sIdCounter = new AtomicInteger(0);
    private int mId;

    private static RTToolbarListener mListener;

    private ViewGroup mToolbarContainer;

    private boolean isBoldChecked = false;
    private boolean isItalicsChecked = false;
    private boolean isStrikeChecked = false;
    private boolean isLinkChecked = false;
    private boolean isBulletChecked = false;
    private boolean isNumbersChecked = false;
    /*
     * The buttons
     */
    public ImageView mBold;
    public ImageView mItalicize;
    public ImageView mStrike;
    public ImageView mLink;
    public ImageView mBullet;
    public ImageView mNumbers;


    /**
     * The attributes
     */

    float horizontalScrollHeight = 0;
    float horizontalScrollMarginTop = 0;
    float horizontalScrollMarginBottom = 0;
    float horizontalScrollMarginLeft = 0;
    float horizontalScrollMarginRight = 0;
    Drawable boldActionDrawable;
    Drawable italicizeActionDrawable;
    Drawable strikeActionDrawable;
    Drawable linkActionDrawable;
    Drawable bulletActionDrawable;
    Drawable numberActionDrawable;
    Drawable selectedActionBackground;
    Drawable unSelectedActionBackground;
    LinearLayout editorScrollView;
    int actionButtonTint;
    int actionButtonSelectedSrcTint;
    float actionButtonPadding = 0;
    float actionButtonSize = 0;


    // ****************************************** Initialize Methods *******************************************

    public HorizontalRTToolbar(Context context) {
        super(context);
        init();

    }

    public HorizontalRTToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttributes(attrs);
        init();
    }

    public HorizontalRTToolbar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseAttributes(attrs);
        init();
    }

    private void init() {
        synchronized (sIdCounter) {
            mId = sIdCounter.getAndIncrement();
        }

        inflateAndBindView();


    }


    private void parseAttributes(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.ctr, 0, 0);

        try {

            horizontalScrollHeight = a.getDimension(R.styleable.ctr_setHorizontalScrollHeight, 0);
            horizontalScrollMarginBottom = a.getDimension(R.styleable.ctr_setHorizontalScrollMarginBottom, 0);
            horizontalScrollMarginTop = a.getDimension(R.styleable.ctr_setHorizontalScrollMarginTop, 0);
            horizontalScrollMarginRight = a.getDimension(R.styleable.ctr_setHorizontalScrollMarginRight, 0);
            horizontalScrollMarginLeft = a.getDimension(R.styleable.ctr_setHorizontalScrollMarginLeft, 0);
            boldActionDrawable = a.getDrawable(R.styleable.ctr_setBoldActionSrc);
            italicizeActionDrawable = a.getDrawable(R.styleable.ctr_setItalicizeActionSrc);
            strikeActionDrawable = a.getDrawable(R.styleable.ctr_setStrikeActionSrc);
            linkActionDrawable = a.getDrawable(R.styleable.ctr_setLinkActionSrc);
            bulletActionDrawable = a.getDrawable(R.styleable.ctr_setBulletActionSrc);
            numberActionDrawable = a.getDrawable(R.styleable.ctr_setNumberActionSrc);
            selectedActionBackground = a.getDrawable(R.styleable.ctr_setSelectedActionBackground);
            unSelectedActionBackground = a.getDrawable(R.styleable.ctr_setUnselectedActionBackground);
            actionButtonPadding = a.getDimension(R.styleable.ctr_setActionButtonPadding, 0);
            actionButtonSize = a.getDimension(R.styleable.ctr_setActionButtonSize, 0);
            actionButtonTint = a.getColor(R.styleable.ctr_setActionButtonSrcTint, Color.BLACK);
            actionButtonSelectedSrcTint = a.getColor(R.styleable.ctr_setActionSelectedButtonSrcTint, Color.WHITE);
        } finally {
            a.recycle();
        }
    }


    protected void inflateAndBindView() {

        View view;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        view = inflater.inflate(R.layout.editor_layout, this, true);
        mBold = view.findViewById(R.id.bold_action);
        mBullet = view.findViewById(R.id.bullet_action);
        mItalicize = view.findViewById(R.id.italics_action);
        mLink = view.findViewById(R.id.link_action);
        mNumbers = view.findViewById(R.id.numbers_action);
        mStrike = view.findViewById(R.id.strike_action);
        mBold.setImageDrawable(boldActionDrawable);
        mBullet.setImageDrawable(bulletActionDrawable);
        mItalicize.setImageDrawable(italicizeActionDrawable);
        mLink.setImageDrawable(linkActionDrawable);
        mNumbers.setImageDrawable(numberActionDrawable);
        mStrike.setImageDrawable(strikeActionDrawable);
        mStrike.setBackground(unSelectedActionBackground);
        mBold.setBackground(unSelectedActionBackground);
        mBullet.setBackground(unSelectedActionBackground);
        mItalicize.setBackground(unSelectedActionBackground);
        mLink.setBackground(unSelectedActionBackground);
        mNumbers.setBackground(unSelectedActionBackground);
        editorScrollView = view.findViewById(R.id.editor_scrollview);
        mBold.setOnClickListener(this);
        mBullet.setOnClickListener(this);
        mItalicize.setOnClickListener(this);
        mLink.setOnClickListener(this);
        mNumbers.setOnClickListener(this);
        mStrike.setOnClickListener(this);
        mBold.setColorFilter(actionButtonTint);
        mNumbers.setColorFilter(actionButtonTint);
        mLink.setColorFilter(actionButtonTint);
        mItalicize.setColorFilter(actionButtonTint);
        mBullet.setColorFilter(actionButtonTint);
        mStrike.setColorFilter(actionButtonTint);


        if (actionButtonPadding != 0) {
            int dpPadding = (int) PixelsToDp(actionButtonPadding, getContext());
            mBold.setPadding(dpPadding, dpPadding, dpPadding, dpPadding);
            mItalicize.setPadding(dpPadding, dpPadding, dpPadding, dpPadding);
            mStrike.setPadding(dpPadding, dpPadding, dpPadding, dpPadding);
            mLink.setPadding(dpPadding, dpPadding, dpPadding, dpPadding);
            mBullet.setPadding(dpPadding, dpPadding, dpPadding, dpPadding);
            mNumbers.setPadding(dpPadding, dpPadding, dpPadding, dpPadding);
        }

        if (actionButtonSize != 0) {
            int dpSize = (int) PixelsToDp(actionButtonSize, getContext());
            mBold.getLayoutParams().height = dpSize;
            mBold.getLayoutParams().width = dpSize;
            mItalicize.getLayoutParams().height = dpSize;
            mItalicize.getLayoutParams().width = dpSize;
            mStrike.getLayoutParams().height = dpSize;
            mStrike.getLayoutParams().width = dpSize;// mStrike.requestLayout();
            mLink.getLayoutParams().height = dpSize;
            mLink.getLayoutParams().width = dpSize; //mLink.requestLayout();
            mBullet.getLayoutParams().height = dpSize;
            mBullet.getLayoutParams().width = dpSize; //mBullet.requestLayout();
            mNumbers.getLayoutParams().height = dpSize;
            mNumbers.getLayoutParams().width = dpSize; //mNumbers.requestLayout();

            editorScrollView.requestLayout();
        }


        if (horizontalScrollMarginLeft != 0) {

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) editorScrollView.getLayoutParams();
            params.leftMargin = (int) PixelsToDp(horizontalScrollMarginLeft, getContext());
            editorScrollView.requestLayout();
        }

        if (horizontalScrollMarginRight != 0) {

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) editorScrollView.getLayoutParams();
            params.rightMargin = (int) PixelsToDp(horizontalScrollMarginRight, getContext());
            editorScrollView.requestLayout();
        }
        if (horizontalScrollMarginTop != 0) {

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) editorScrollView.getLayoutParams();
            params.topMargin = (int) PixelsToDp(horizontalScrollMarginTop, getContext());
            editorScrollView.requestLayout();
        }
        if (horizontalScrollMarginBottom != 0) {

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) editorScrollView.getLayoutParams();
            params.bottomMargin = (int) PixelsToDp(horizontalScrollMarginBottom, getContext());
            editorScrollView.requestLayout();
        }
        if (horizontalScrollHeight != 0) {

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) editorScrollView.getLayoutParams();
            params.height = (int) PixelsToDp(horizontalScrollHeight, getContext());
            editorScrollView.requestLayout();
        }
    }

    // ****************************************** RTToolbar Methods *******************************************

    public float PixelsToDp(float px, Context context) {

        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    @Override
    public void setToolbarContainer(ViewGroup toolbarContainer) {
        mToolbarContainer = toolbarContainer;
    }

    @Override
    public ViewGroup getToolbarContainer() {
        return mToolbarContainer == null ? this : mToolbarContainer;
    }

    @Override
    public void setToolbarListener(RTToolbarListener listener) {
        mListener = listener;
    }

    @Override
    public void removeToolbarListener() {
        mListener = null;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setBold(boolean enabled) {

        setBoldChecked(enabled);
        authBackground(mBold, enabled);
    }

    @Override
    public void setItalic(boolean enabled) {

        setItalicsChecked(enabled);
        authBackground(mItalicize, enabled);
    }

    @Override
    public void setUnderline(boolean enabled) {
        //if (mUnderline != null) mUnderline.setChecked(enabled);
    }

    @Override
    public void setStrikethrough(boolean enabled) {

        setStrikeChecked(enabled);
        authBackground(mStrike, enabled);
    }

    @Override
    public void setFontColor(int color) {

    }


    @Override
    public void setBullet(boolean enabled) {
        setBulletChecked(enabled);
        authBackground(mBullet, enabled);
    }

    @Override
    public void setNumber(boolean enabled) {
        setNumbersChecked(enabled);
        authBackground(mNumbers, enabled);
    }


    @Override
    public void setAlignment(Layout.Alignment alignment) {

    }

    private void setBoldChecked(boolean checkStatus) {

        this.isBoldChecked = checkStatus;

    }

    private void setItalicsChecked(boolean checkStatus) {

        this.isItalicsChecked = checkStatus;

    }

    public void setStrikeChecked(boolean strikeChecked) {

        isStrikeChecked = strikeChecked;
    }

    public void setLinkChecked(boolean linkChecked) {
        isLinkChecked = linkChecked;
    }

    public void setBulletChecked(boolean bulletChecked) {
        isBulletChecked = bulletChecked;
    }

    public void setNumbersChecked(boolean numbersChecked) {
        isNumbersChecked = numbersChecked;
    }

    private boolean isBoldChecked() {
        return isBoldChecked;
    }

    public boolean isItalicsChecked() {
        return isItalicsChecked;
    }

    public boolean isLinkChecked() {
        return isLinkChecked;
    }

    public boolean isStrikeChecked() {
        return isStrikeChecked;
    }

    public boolean isBulletChecked() {
        return isBulletChecked;
    }

    public boolean isNumbersChecked() {

        return isNumbersChecked;
    }

    private void authBackground(ImageView imageView, boolean checkStatus) {

        if (checkStatus) {
            imageView.setBackground(selectedActionBackground);
            imageView.setColorFilter(actionButtonSelectedSrcTint);
        } else {

            imageView.setBackground(unSelectedActionBackground);
            imageView.setColorFilter(actionButtonTint);

        }

    }


    // ****************************************** Item Selected Methods *******************************************


    @Override
    public void onClick(View v) {


        if (mListener != null) {

            int id = v.getId();
            if (id == R.id.bold_action) {

                setBoldChecked(!isBoldChecked);
                authBackground(mBold, isBoldChecked);


                mListener.onEffectSelected(Effects.BOLD, isBoldChecked());
            } else if (id == R.id.italics_action) {
                setItalicsChecked(!isItalicsChecked);
                authBackground(mItalicize, isItalicsChecked());
                mListener.onEffectSelected(Effects.ITALIC, isItalicsChecked());
            } else if (id == R.id.strike_action) {

                setStrikeChecked(!isStrikeChecked);
                authBackground(mStrike, isStrikeChecked());
                mListener.onEffectSelected(Effects.STRIKETHROUGH, isStrikeChecked());
            } else if (id == R.id.numbers_action) {

                setNumbersChecked(!isNumbersChecked);
                authBackground(mNumbers, isNumbersChecked());
                mListener.onEffectSelected(Effects.NUMBER, isNumbersChecked());
            }
            else if (id == R.id.bullet_action) {

                setBulletChecked(!isBulletChecked);
                authBackground(mBullet, isBulletChecked());
                //mListener.onEffectSelected(Effects.BULLET, isBulletChecked());
                mListener.onEffectSelected(Effects.FONTCOLOR, Color.parseColor("#465ED3"));

            } else if (id == R.id.link_action) {

                mListener.onCreateLink();

            }


        }

    }




}
