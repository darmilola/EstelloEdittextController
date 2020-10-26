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


import com.deltastream.example.edittextcontroller.effects.Effect;
import com.deltastream.example.edittextcontroller.spans.RTSpan;

/*
 * Listener interface to listen to toolbar events like bold selected/unselected.
 * The methods should be fairly self-explaining.
 */
public interface RTToolbarListener {


    <V, C extends RTSpan<V>> void onEffectSelected(Effect<V, C> effect, V value);

    void onUndo();

    void onRedo();

    void onClearFormatting();

    void onCreateLink();


    void onMentionSelected();
    void onHashTagSelected();
    void onStopMentioning();
    void onStopHashTags();



}
