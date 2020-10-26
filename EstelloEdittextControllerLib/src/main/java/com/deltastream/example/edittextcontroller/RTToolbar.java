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

import android.text.Layout;
import android.view.ViewGroup;



/**
 * An interface describing a rich text toolbar.
 * <p>
 * There are methods to set/clear effects (like bold on/off) and
 * there are callback methods to let the listener know if the user
 * selected some effect on the toolbar.
 */
public interface RTToolbar {

    /**
     * Set this toolbar's listener.
     * There can be only one since this should be the RTManager and there's only
     * one of those (per layout).
     */
    void setToolbarListener(RTToolbarListener listener);

    /**
     * Remove this toolbar's listener.
     */
    void removeToolbarListener();

    /**
     * We can have more than one toolbar identified by this unique Id
     * (unique per layout).
     * It can be implemented e.g. by using a static counter.
     */
    int getId();

    /**
     * This is merely a way to store the container in which the toolbar is shown
     * since we might need to hide/show the toolbar which happens by
     * hiding/showing the container.
     *
     * @param toolbarContainer The ViewGroup that contains this RTToolbar.
     */
    void setToolbarContainer(ViewGroup toolbarContainer);

    /**
     * @return The ViewGroup that contains this RTToolbar.
     */
    ViewGroup getToolbarContainer();

    void setBold(boolean enabled);

    void setItalic(boolean enabled);

    void setUnderline(boolean enabled);

    void setStrikethrough(boolean enabled);


    void setFontColor(int color);

    void setBullet(boolean enabled);

    void setNumber(boolean enabled);

    void setAlignment(Layout.Alignment alignment);





}