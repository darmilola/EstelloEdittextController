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

package com.deltastream.example.edittextcontroller.api.format;

import android.text.Editable;
import android.view.inputmethod.BaseInputConnection;

import com.deltastream.example.edittextcontroller.RTEditText;
import com.deltastream.example.edittextcontroller.converter.ConverterSpannedToHtml;


public final class RTEditable extends RTSpanned {

    private RTEditText mEditor;

    public RTEditable(RTEditText editor) {
        super(editor.getText());
        mEditor = editor;
    }

    @Override
    public RTText convertTo(RTFormat destFormat) {
        if (destFormat instanceof RTFormat.Html) {
            clean();
            return new ConverterSpannedToHtml().convert(mEditor.getText(), (RTFormat.Html) destFormat);
        } else if (destFormat instanceof RTFormat.PlainText) {
            clean();
            RTHtml  rtHtml = new ConverterSpannedToHtml().convert(mEditor.getText(), RTFormat.HTML);
            RTText rtText = rtHtml.convertTo(RTFormat.PLAIN_TEXT);
            return new RTPlainText(rtText.getText());
        }

        return super.convertTo(destFormat);
    }

    private void clean() {
        Editable text = mEditor.getText();
        BaseInputConnection.removeComposingSpans(text);


    }
}