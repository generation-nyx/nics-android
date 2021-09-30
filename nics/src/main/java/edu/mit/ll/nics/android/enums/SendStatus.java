/*
 * Copyright (c) 2008-2021, Massachusetts Institute of Technology (MIT)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.mit.ll.nics.android.enums;

/**
 * @author Glenn L. Primmer
 * <p>
 * This defines the enumeration of the different Report Statuses.
 */
public enum SendStatus {
    /**
     * Ready to send.
     */
    WAITING_TO_SEND(0, "Waiting to send"),

    /**
     * Report has sent.
     */
    SENT(1, "Sent"),

    /**
     * Report is received.
     */
    RECEIVED(2, "Received"),

    /**
     * Report update is sent..
     */
    UPDATE(3, "update"),

    /**
     * Report is received.
     */
    DELETE(4, "delete"),

    DELETING(5, "deleting"),

    UPDATING(6, "updating"),

    SAVED(7, "saved");

    /**
     * The report status numerical value (safer than using ordinal)
     */
    private final int id;

    /**
     * The report status textual representation.
     */
    private final String text;

    /**
     * Enumeration constructor, extended so that textual representation and an integer value
     * can be defined (safer than use of ordinal values).
     *
     * @param id   The numerical representation of the enumerated value.
     * @param text The textual representation of the enumerated value.
     */
    SendStatus(int id, String text) {
        this.id = id;
        this.text = text;
    }

    /**
     * Gets the numerical representation of the enumerated value.
     *
     * @return The numerical representation of the enumerated value.
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the textual representation of the enumerated value.
     *
     * @return The textual representation of the enumerated value.
     */
    private String getText() {
        return text;
    }

    /**
     * Looks up an Log Level for the provided numerical representation of a report status.
     *
     * @param id Numerical representation of a Report Status.
     * @return The Report Status that corresponds to the numerical representation provided.  If there is no Report Status
     * that corresponds to the numerical representation provided then 'null' will be returned.
     */
    public static SendStatus lookUp(int id) {
        SendStatus value = null;

        for (SendStatus status : SendStatus.values()) {
            if (status.getId() == id) {
                value = status;

                break;
            }
        }

        return value;
    }

    /**
     * Looks up an Report Status for the provided textual representation of a Report Status.
     *
     * @param text Textual representation of an Report Status.
     * @return The Report Status that corresponds to the textual representation provided.  If there is no Report Status
     * that corresponds to the textual representation provided then 'null' will be returned.
     */
    public static SendStatus lookUp(String text) {
        SendStatus value = null;

        for (SendStatus status : SendStatus.values()) {
            if (status.getText().equals(text)) {
                value = status;

                break;
            }
        }

        return value;
    }
}