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
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;

public class User {

    @SerializedName("userId")
    private long userId;

    @SerializedName("username")
    private String userName;

    @SerializedName("firstname")
    private String firstName;

    @SerializedName("lastname")
    private String lastName;

    @SerializedName("rank")
    private String rank;

    @SerializedName("primaryMobilePhone")
    private String primaryMobilePhone;

    @SerializedName("primaryHomePhone")
    private String primaryHomePhone;

    @SerializedName("primaryEmailAddr")
    private String primaryEmailAddr;

    @SerializedName("homeBaseName")
    private String homeBaseName;

    @SerializedName("homeBaseStreet")
    private String homeBaseStreet;

    @SerializedName("homeBaseCity")
    private String homeBaseCity;

    @SerializedName("homeBaseState")
    private String homeBaseState;

    @SerializedName("homeBaseZip")
    private String homeBaseZip;

    @SerializedName("agency")
    private String agency;

    @SerializedName("approxWeight")
    private int approxWeight;

    @SerializedName("remarks")
    private String remarks;

    @SerializedName("qualifiedPositions")
    private ArrayList<Integer> qualifiedPositions;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof User)) return false;

        User user = (User) o;

        return new EqualsBuilder()
                .append(getUserId(), user.getUserId())
                .append(getUserName(), user.getUserName())
                .append(getFirstName(), user.getFirstName())
                .append(getLastName(), user.getLastName())
                .append(getRank(), user.getRank())
                .append(getPrimaryMobilePhone(), user.getPrimaryMobilePhone())
                .append(getPrimaryHomePhone(), user.getPrimaryHomePhone())
                .append(getPrimaryEmailAddr(), user.getPrimaryEmailAddr())
                .append(getHomeBaseName(), user.getHomeBaseName())
                .append(getHomeBaseStreet(), user.getHomeBaseStreet())
                .append(getHomeBaseCity(), user.getHomeBaseCity())
                .append(getHomeBaseState(), user.getHomeBaseState())
                .append(getHomeBaseZip(), user.getHomeBaseZip())
                .append(getAgency(), user.getAgency())
                .append(getApproxWeight(), user.getApproxWeight())
                .append(getRemarks(), user.getRemarks())
                .append(getQualifiedPositions(), user.getQualifiedPositions())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getUserId())
                .append(getUserName())
                .append(getFirstName())
                .append(getLastName())
                .append(getRank())
                .append(getPrimaryMobilePhone())
                .append(getPrimaryHomePhone())
                .append(getPrimaryEmailAddr())
                .append(getHomeBaseName())
                .append(getHomeBaseStreet())
                .append(getHomeBaseCity())
                .append(getHomeBaseState())
                .append(getHomeBaseZip())
                .append(getAgency())
                .append(getApproxWeight())
                .append(getRemarks())
                .append(getQualifiedPositions())
                .toHashCode();
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getPrimaryMobilePhone() {
        return primaryMobilePhone;
    }

    public void setPrimaryMobilePhone(String primaryMobilePhone) {
        this.primaryMobilePhone = primaryMobilePhone;
    }

    public String getPrimaryHomePhone() {
        return primaryHomePhone;
    }

    public void setPrimaryHomePhone(String primaryHomePhone) {
        this.primaryHomePhone = primaryHomePhone;
    }

    public String getPrimaryEmailAddr() {
        return primaryEmailAddr;
    }

    public void setPrimaryEmailAddr(String primaryEmailAddr) {
        this.primaryEmailAddr = primaryEmailAddr;
    }

    public String getHomeBaseName() {
        return homeBaseName;
    }

    public void setHomeBaseName(String homeBaseName) {
        this.homeBaseName = homeBaseName;
    }

    public String getHomeBaseStreet() {
        return homeBaseStreet;
    }

    public void setHomeBaseStreet(String homeBaseStreet) {
        this.homeBaseStreet = homeBaseStreet;
    }

    public String getHomeBaseCity() {
        return homeBaseCity;
    }

    public void setHomeBaseCity(String homeBaseCity) {
        this.homeBaseCity = homeBaseCity;
    }

    public String getHomeBaseState() {
        return homeBaseState;
    }

    public void setHomeBaseState(String homeBaseState) {
        this.homeBaseState = homeBaseState;
    }

    public String getHomeBaseZip() {
        return homeBaseZip;
    }

    public void setHomeBaseZip(String homeBaseZip) {
        this.homeBaseZip = homeBaseZip;
    }

    public ArrayList<Integer> getQualifiedPositions() {
        return qualifiedPositions;
    }

    public void setQualifiedPositions(ArrayList<Integer> qualifiedPositions) {
        this.qualifiedPositions = qualifiedPositions;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public int getApproxWeight() {
        return approxWeight;
    }

    public void setApproxWeight(int approxWeight) {
        this.approxWeight = approxWeight;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
