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
package edu.mit.ll.nics.android.utils;

import edu.mit.ll.nics.android.database.entities.OrgCapability;
import edu.mit.ll.nics.android.database.entities.OrgCapabilityData;
import edu.mit.ll.nics.android.data.OrgCapabilities;

@SuppressWarnings("FieldCanBeLocal")
public class MyOrgCapabilities {

    private final String rescKey = "RESC";    //Resource
    private final String abcKey = "ABC";        //ABC Form
    private final String two15Key = "215";    //215 Form
    private final String sitrepKey = "SITREP";    //SITREP Form
    private final String assignKey = "ASSGN";        //ASSGN
    private final String srKey = "SR";        //General Message Report	--Mobile
    private final String eodKey = "EOD"; //EOD Report                  --Mobile
    private final String frKey = "FR";        //Field Report				--Mobile
    private final String taskKey = "TASK";    //Task Report
    private final String resreqKey = "RESREQ";    //Resource Request		--Mobile
    private final String rocKey = "ROC";        //ROC Report
    private final String nine110Key = "9110";    //9110
    private final String uxoKey = "UXO";        //Explosives Report			--Mobile
    private final String dmgrptKey = "DMGRPT";    //Damage Report				--Mobile
    private final String mitamKey = "MITAM";    //MITAM Report
    private final String wrKey = "WR";    //Weather Report				--Mobile
    private final String mivKey = "MIV";    //Multi-Incident View
    private final String exportKey = "EXPORT";    //Export Data layers
    private final String cencsusKey = "CENSUS";    //Census Application
    private final String waKey = "WA";    //WA
    private final String two04Key = "204"; //204 Form						--Mobile
    private final String garKey = "GAR";    //GAR Report					--Mobile
    private final String two07Key = "207";    //207 Report
    private final String two01Key = "201";    //201 Report
    private final String two03Key = "203";    //203 Report
    private final String agrrptKey = "AGRRPT";    //AGRRPT Report			--Mobile
    private final String two04aKey = "204A";    //204a Report
    private final String two02Key = "202";    //202 Report
    private final String two05Key = "205";    //205 Report
    private final String two06Key = "206";    //206 Report
    private final String chatKey = "CHAT";    //Chat						--Mobile	//double check with web

    private boolean RESC = false;
    private boolean ABC = false;
    private boolean two15 = false;
    private boolean SITREP = false;
    private boolean ASSGN = false;
    private boolean SR = false;
    private boolean EOD = false;
    private boolean FR = false;
    private boolean TASK = false;
    private boolean RESREQ = false;
    private boolean ROC = false;
    private boolean nine110 = false;
    private boolean UXO = false;
    private boolean DMGRPT = false;
    private boolean MITAM = false;
    private boolean WR = false;
    private boolean MIV = false;
    private boolean CENSUS = false;
    private boolean WA = false;
    private boolean two04 = false;
    private boolean GAR = false;
    private boolean two07 = false;
    private boolean two01 = false;
    private boolean two03 = false;
    private boolean AGRRPT = false;
    private boolean two04A = false;
    private boolean two02 = false;
    private boolean two05 = false;
    private boolean two06 = false;
    private boolean CHAT = false;

    public void setCapabilities(OrgCapabilities capabilities) {
        resetCapabilitiesToOff();

        OrgCapability[] caps = capabilities.getOrgCaps();

        if (caps != null) {
            for (OrgCapability cap : caps) {
                OrgCapabilityData data = cap.getCap();

                String name = data.getName();

                if (name != null) {
                    switch (name) {
                        case srKey:
                            SR = cap.isActiveMobile();
                            break;
                        case eodKey:
                            EOD = cap.isActiveMobile();
                            break;
                        case frKey:
                            FR = cap.isActiveMobile();
                            break;
                        case resreqKey:
                            RESREQ = cap.isActiveMobile();
                            break;
                        case uxoKey:
                            UXO = cap.isActiveMobile();
                            break;
                        case dmgrptKey:
                            DMGRPT = cap.isActiveMobile();
                            break;
                        case wrKey:
                            WR = cap.isActiveMobile();
                            break;
                        case two04Key:
                            two04 = cap.isActiveMobile();
                            break;
                        case garKey:
                            GAR = cap.isActiveMobile();
                            break;
                        case agrrptKey:
                            AGRRPT = cap.isActiveMobile();
                            break;
                    }
                }
            }
        }

        CHAT = true;
        RESREQ = false;
        FR = false;
        AGRRPT = false;
        EOD = true;
//		UXO = true;
    }

    public void resetCapabilitiesToOff() {
        RESC = false;
        ABC = false;
        two15 = false;
        SITREP = false;
        ASSGN = false;
        SR = false;
        EOD = false;
        FR = false;
        TASK = false;
        RESREQ = false;
        ROC = false;
        nine110 = false;
        UXO = false;
        DMGRPT = false;
        MITAM = false;
        WR = false;
        MIV = false;
        CENSUS = false;
        WA = false;
        two04 = false;
        GAR = false;
        two07 = false;
        two01 = false;
        two03 = false;
        AGRRPT = false;
        two04A = false;
        two02 = false;
        two05 = false;
        two06 = false;
    }

    public void resetCapabilitiesToOn() {
        RESC = true;
        ABC = true;
        two15 = true;
        SITREP = true;
        ASSGN = true;
        SR = true;
        EOD = true;
        FR = true;
        TASK = true;
        RESREQ = true;
        ROC = true;
        nine110 = true;
        UXO = true;
        DMGRPT = true;
        MITAM = true;
        WR = true;
        MIV = true;
        CENSUS = true;
        WA = true;
        two04 = true;
        GAR = true;
        two07 = true;
        two01 = true;
        two03 = true;
        AGRRPT = true;
        two04A = true;
        two02 = true;
        two05 = true;
        two06 = true;
    }

    public boolean isRESC() {
        return RESC;
    }

    public void setRESC(boolean rESC) {
        RESC = rESC;
    }

    public boolean isABC() {
        return ABC;
    }

    public void setABC(boolean aBC) {
        ABC = aBC;
    }

    public boolean isTwo15() {
        return two15;
    }

    public void setTwo15(boolean two15) {
        this.two15 = two15;
    }

    public boolean isSITREP() {
        return SITREP;
    }

    public void setSITREP(boolean sITREP) {
        SITREP = sITREP;
    }

    public boolean isASSGN() {
        return ASSGN;
    }

    public void setASSGN(boolean aSSGN) {
        ASSGN = aSSGN;
    }

    public boolean isSR() {
        return SR;
    }

    public boolean isEOD() {
        return EOD;
    }

    public void setEOD(boolean eod) {
        EOD = eod;
    }

    public void setSR(boolean sR) {
        SR = sR;
    }

    public boolean isFR() {
        return FR;
    }

    public void setFR(boolean fR) {
        FR = fR;
    }

    public boolean isTASK() {
        return TASK;
    }

    public void setTASK(boolean tASK) {
        TASK = tASK;
    }

    public boolean isRESREQ() {
        return RESREQ;
    }

    public void setRESREQ(boolean rESREQ) {
        RESREQ = rESREQ;
    }

    public boolean isROC() {
        return ROC;
    }

    public void setROC(boolean rOC) {
        ROC = rOC;
    }

    public boolean isNine110() {
        return nine110;
    }

    public void setNine110(boolean nine110) {
        this.nine110 = nine110;
    }

    public boolean isUXO() {
        return UXO;
    }

    public void setUXO(boolean uXO) {
        UXO = uXO;
    }

    public boolean isDMGRPT() {
        return DMGRPT;
    }

    public void setDMGRPT(boolean dMGRPT) {
        DMGRPT = dMGRPT;
    }

    public boolean isMITAM() {
        return MITAM;
    }

    public void setMITAM(boolean mITAM) {
        MITAM = mITAM;
    }

    public boolean isWR() {
        return WR;
    }

    public void setWR(boolean wR) {
        WR = wR;
    }

    public boolean isMIV() {
        return MIV;
    }

    public void setMIV(boolean mIV) {
        MIV = mIV;
    }

    public boolean isCENSUS() {
        return CENSUS;
    }

    public void setCENSUS(boolean cENSUS) {
        CENSUS = cENSUS;
    }

    public boolean isWA() {
        return WA;
    }

    public void setWA(boolean wA) {
        WA = wA;
    }

    public boolean isTwo04() {
        return two04;
    }

    public void setTwo04(boolean two04) {
        this.two04 = two04;
    }

    public boolean isGAR() {
        return GAR;
    }

    public void setGAR(boolean gAR) {
        GAR = gAR;
    }

    public boolean isTwo07() {
        return two07;
    }

    public void setTwo07(boolean two07) {
        this.two07 = two07;
    }

    public boolean isTwo01() {
        return two01;
    }

    public void setTwo01(boolean two01) {
        this.two01 = two01;
    }

    public boolean isTwo03() {
        return two03;
    }

    public void setTwo03(boolean two03) {
        this.two03 = two03;
    }

    public boolean isAGRRPT() {
        return AGRRPT;
    }

    public void setAGRRPT(boolean aGRRPT) {
        AGRRPT = aGRRPT;
    }

    public boolean isTwo04A() {
        return two04A;
    }

    public void setTwo04A(boolean two04a) {
        two04A = two04a;
    }

    public boolean isTwo02() {
        return two02;
    }

    public void setTwo02(boolean two02) {
        this.two02 = two02;
    }

    public boolean isTwo05() {
        return two05;
    }

    public void setTwo05(boolean two05) {
        this.two05 = two05;
    }

    public boolean isTwo06() {
        return two06;
    }

    public void setTwo06(boolean two06) {
        this.two06 = two06;
    }

    public boolean isCHAT() {
        return CHAT;
    }

    public void setCHAT(boolean cHAT) {
        CHAT = cHAT;
    }
}
