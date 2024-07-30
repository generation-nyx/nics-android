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
package edu.mit.ll.nics.android.database.entities;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;
import java.util.Map;

import edu.mit.ll.nics.android.maps.layers.LayerType;
import edu.mit.ll.nics.android.utils.gson.NestedJSONTypeAdapter;

import static edu.mit.ll.nics.android.utils.constants.Database.COLLABROOM_LAYERS_TABLE;

@Entity(tableName = COLLABROOM_LAYERS_TABLE, indices = {@Index(value = {"datalayerId"}, unique = true)})
public class CollabroomDataLayer {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @SerializedName("datalayerid")
    private String datalayerId;

    @SerializedName("displayname")
    private String displayName;

    @SerializedName("created")
    private String created;

    @SerializedName("collabroomId")
    private long collabroomId;

    @Embedded
    @SerializedName("datalayersource")
    private DataLayerSource datalayerSource;

    @Ignore
    private List<EmbeddedCollabroomDatalayer> collabroomDatalayers;

    @Ignore
    private List<LayerFeature> features;

    private boolean active = false;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void toggle() {
        this.active = !active;
    }

    public String getDatalayerId() {
        return datalayerId;
    }

    public void setDatalayerId(String datalayerId) {
        this.datalayerId = datalayerId;
    }

    public long getCollabroomDatalayerId() {
        return this.getCollabroomDatalayers().get(0).getCollabroomDatalayerId();
    }

    public HazardInfo getHazardInfo() {
        return this.getCollabroomDatalayers().get(0).getHazard();
    }

    public List<LayerFeature> getFeatures() {
        return features;
    }

    public void setFeatures(List<LayerFeature> features) {
        this.features = features;
    }

    public DataLayerSource getDatalayerSource() {
        return datalayerSource;
    }

    public void setDatalayerSource(DataLayerSource datalayerSource) {
        this.datalayerSource = datalayerSource;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getTypeName() {
        return this.getDatalayerSource().getDataSource().getDataSourceType().getTypeName();
    }

    public String getInternalUrl() {
        return this.getDatalayerSource().getDataSource().getInternalUrl();
    }

    public int getRefreshRate() {
        return this.getDatalayerSource().getRefreshRate();
    }

    public String getLayername() {
        return this.getDatalayerSource().getLayerName();
    }

    public List<EmbeddedCollabroomDatalayer> getCollabroomDatalayers() {
        return collabroomDatalayers;
    }

    public void setCollabroomDatalayers(List<EmbeddedCollabroomDatalayer> collabroomDatalayers) {
        this.collabroomDatalayers = collabroomDatalayers;
    }

    public boolean isMobileEnabled() {
        return this.getCollabroomDatalayers().get(0).isEnableMobile();
    }

    public double getOpacity() {
        return this.getCollabroomDatalayers().get(0).getCollabroomOpacity();
    }

    public long getCollabroomId() {
        return collabroomId;
    }

    public void setCollabroomId(long collabroomId) {
        this.collabroomId = collabroomId;
    }

    public boolean hasFeatures() {
        LayerType type = LayerType.lookUp(getTypeName());
        if (type == null) {
            return false;
        }
        return type.equals(LayerType.WFS) || type.equals(LayerType.GEOJSON);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CollabroomDataLayer)) return false;

        CollabroomDataLayer layer = (CollabroomDataLayer) o;

        return new EqualsBuilder()
                .append(getDatalayerId(), layer.getDatalayerId())
                .append(getDisplayName(), layer.getDisplayName())
                .append(getCreated(), layer.getCreated())
                .append(getCollabroomId(), layer.getCollabroomId())
                .append(getDatalayerSource(), layer.getDatalayerSource())
                .append(getCollabroomDatalayers(), layer.getCollabroomDatalayers())
                .append(getFeatures(), layer.getFeatures())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getDatalayerId())
                .append(getDisplayName())
                .append(getCreated())
                .append(getCollabroomId())
                .append(getDatalayerSource())
                .append(getCollabroomDatalayers())
                .append(getFeatures())
                .toHashCode();
    }

    public static class DataLayerSource {

        @Embedded
        @SerializedName("datasource")
        private DataSource dataSource;

        @SerializedName("refreshrate")
        private int refreshRate;

        @SerializedName("layername")
        private String layerName;

        @JsonAdapter(NestedJSONTypeAdapter.class)
        @SerializedName("attributes")
        private Map<String, String> attributes;

        public DataSource getDataSource() {
            return dataSource;
        }

        public void setDataSource(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        public int getRefreshRate() {
            return refreshRate;
        }

        public void setRefreshRate(int refreshRate) {
            this.refreshRate = refreshRate;
        }

        public String getLayerName() {
            return layerName;
        }

        public void setLayerName(String layerName) {
            this.layerName = layerName;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof DataLayerSource)) return false;

            DataLayerSource source = (DataLayerSource) o;

            return new EqualsBuilder()
                    .append(getDataSource(), source.getDataSource())
                    .append(getRefreshRate(), source.getRefreshRate())
                    .append(getLayerName(), source.getLayerName())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(getDataSource())
                    .append(getRefreshRate())
                    .append(getLayerName())
                    .toHashCode();
        }
    }

    public static class DataSource {

        @Embedded
        @SerializedName("datasourcetype")
        private DataSourceType dataSourceType;

        @SerializedName("internalurl")
        private String internalUrl;

        public DataSourceType getDataSourceType() {
            return dataSourceType;
        }

        public void setDataSourceType(DataSourceType dataSourceType) {
            this.dataSourceType = dataSourceType;
        }

        public String getInternalUrl() {
            return internalUrl;
        }

        public void setInternalUrl(String internalUrl) {
            this.internalUrl = internalUrl;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof DataSource)) return false;

            DataSource dataSource = (DataSource) o;

            return new EqualsBuilder()
                    .append(getDataSourceType(), dataSource.getDataSourceType())
                    .append(getInternalUrl(), dataSource.getInternalUrl())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(getDataSourceType())
                    .append(getInternalUrl())
                    .toHashCode();
        }
    }

    public static class DataSourceType {

        @SerializedName("typename")
        private String typeName;

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof DataSourceType)) return false;

            DataSourceType dataSourceType = (DataSourceType) o;

            return new EqualsBuilder()
                    .append(getTypeName(), dataSourceType.getTypeName())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(getTypeName())
                    .toHashCode();
        }
    }
}
