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


import com.deltastream.example.edittextcontroller.converter.ConverterHtmlToSpanned;
import com.deltastream.example.edittextcontroller.converter.ConverterHtmlToText;

/**
 * RTText representing an html text.
 * <p>
 * The text may contain referenced images.
 * Audio and video files aren't supported yet.
 */
public class RTHtml extends RTText {



    public RTHtml(CharSequence html) {
        this(RTFormat.HTML, html);
    }



    public RTHtml(RTFormat.Html rtFormat, CharSequence html) {
        super(rtFormat, html);

    }

    @Override
    public String getText() {
        CharSequence text = super.getText();
        return text != null ? text.toString() : "";
    }



    @Override
    public RTText convertTo(RTFormat destFormat) {
        if (destFormat instanceof RTFormat.PlainText) {
            return ConverterHtmlToText.convert(this);
        } else if (destFormat instanceof RTFormat.Spanned) {
            return new ConverterHtmlToSpanned().convert(this);
        }

        return super.convertTo(destFormat);
    }

}