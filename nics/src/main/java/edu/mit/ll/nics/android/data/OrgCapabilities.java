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
package edu.mit.ll.nics.android.data;

import com.google.gson.Gson;

import edu.mit.ll.nics.android.database.entities.OrgCapability;

public class OrgCapabilities {

    private String[] orgAdminList;
    private String[] orgTypes;
    private String[] orgOrgTypes;

    private OrgCapability[] orgCaps;

    private String[] caps;
    private int count;
    private String[] organizations;

    public String[] getOrgAdminList() {
        return orgAdminList;
    }

    public void setOrgAdminList(String[] orgAdminList) {
        this.orgAdminList = orgAdminList;
    }

    public String[] getOrgTypes() {
        return orgTypes;
    }

    public void setOrgTypes(String[] orgTypes) {
        this.orgTypes = orgTypes;
    }

    public String[] getOrgOrgTypes() {
        return orgOrgTypes;
    }

    public void setOrgOrgTypes(String[] orgOrgTypes) {
        this.orgOrgTypes = orgOrgTypes;
    }

    public OrgCapability[] getOrgCaps() {
        return orgCaps;
    }

    public void setOrgCaps(OrgCapability[] orgCaps) {
        this.orgCaps = orgCaps;
    }

    public String[] getCaps() {
        return caps;
    }

    public void setCaps(String[] caps) {
        this.caps = caps;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String[] getOrganizations() {
        return organizations;
    }

    public void setOrganizations(String[] organizations) {
        this.organizations = organizations;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
