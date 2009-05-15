/*
 * Copyright (c) 2009. The Codehaus. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.codehaus.httpcache4j.payload;

import org.codehaus.httpcache4j.MIMEType;
import org.apache.commons.lang.Validate;
import org.apache.commons.io.input.ClosedInputStream;

import java.io.InputStream;
import java.io.Serializable;

/**
 * @author <a href="mailto:erlend@escenic.com">Erlend Hamnaberg</a>
 * @version $Revision: #5 $ $Date: 2008/09/15 $
 */
public class ClosedInputStreamPayload implements Payload, Serializable{
    private static final long serialVersionUID = 191318577797232567L;

    private MIMEType mimeType;

    public ClosedInputStreamPayload(final MIMEType mimeType) {
        Validate.notNull(mimeType, "MIMEType may not be null");
        this.mimeType = mimeType;
    }

    public MIMEType getMimeType() {
        return mimeType;
    }

    public InputStream getInputStream() {
        return ClosedInputStream.CLOSED_INPUT_STREAM;
    }

    public boolean isAvailable() {
        return true;
    }

    public boolean isTransient() {
        return false;
    }
}