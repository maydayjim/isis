/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.isis.core.metamodel.services.grid;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.services.layout.GridNormalizerService;
import org.apache.isis.applib.layout.common.ActionLayoutData;
import org.apache.isis.applib.layout.common.ActionLayoutDataOwner;
import org.apache.isis.applib.layout.common.CollectionLayoutData;
import org.apache.isis.applib.layout.common.FieldSet;
import org.apache.isis.applib.layout.common.Grid;
import org.apache.isis.applib.layout.common.MemberRegionOwner;
import org.apache.isis.applib.layout.common.PropertyLayoutData;
import org.apache.isis.core.metamodel.services.grid.fixedcols.applib.FCColumn;
import org.apache.isis.core.metamodel.services.grid.fixedcols.applib.FCColumnOwner;
import org.apache.isis.core.metamodel.services.grid.fixedcols.applib.FCGrid;
import org.apache.isis.core.metamodel.services.grid.fixedcols.applib.FCTab;
import org.apache.isis.applib.services.i18n.TranslationService;
import org.apache.isis.applib.services.jaxb.JaxbService;
import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facetapi.FacetUtil;
import org.apache.isis.core.metamodel.facets.actions.layout.ActionPositionFacetForActionXml;
import org.apache.isis.core.metamodel.facets.actions.layout.BookmarkPolicyFacetForActionXml;
import org.apache.isis.core.metamodel.facets.actions.layout.CssClassFaFacetForActionXml;
import org.apache.isis.core.metamodel.facets.actions.layout.CssClassFacetForActionXml;
import org.apache.isis.core.metamodel.facets.actions.layout.DescribedAsFacetForActionXml;
import org.apache.isis.core.metamodel.facets.actions.layout.HiddenFacetForActionLayoutXml;
import org.apache.isis.core.metamodel.facets.actions.layout.NamedFacetForActionXml;
import org.apache.isis.core.metamodel.facets.collections.layout.CssClassFacetForCollectionXml;
import org.apache.isis.core.metamodel.facets.collections.layout.DefaultViewFacetForCollectionXml;
import org.apache.isis.core.metamodel.facets.collections.layout.DescribedAsFacetForCollectionXml;
import org.apache.isis.core.metamodel.facets.collections.layout.HiddenFacetForCollectionXml;
import org.apache.isis.core.metamodel.facets.collections.layout.NamedFacetForCollectionXml;
import org.apache.isis.core.metamodel.facets.collections.layout.PagedFacetForCollectionXml;
import org.apache.isis.core.metamodel.facets.collections.layout.SortedByFacetForCollectionXml;
import org.apache.isis.core.metamodel.facets.members.order.annotprop.MemberOrderFacetXml;
import org.apache.isis.core.metamodel.facets.properties.propertylayout.CssClassFacetForPropertyXml;
import org.apache.isis.core.metamodel.facets.properties.propertylayout.DescribedAsFacetForPropertyXml;
import org.apache.isis.core.metamodel.facets.properties.propertylayout.HiddenFacetForPropertyXml;
import org.apache.isis.core.metamodel.facets.properties.propertylayout.LabelAtFacetForPropertyXml;
import org.apache.isis.core.metamodel.facets.properties.propertylayout.MultiLineFacetForPropertyXml;
import org.apache.isis.core.metamodel.facets.properties.propertylayout.NamedFacetForPropertyXml;
import org.apache.isis.core.metamodel.facets.properties.propertylayout.RenderedAdjustedFacetForPropertyXml;
import org.apache.isis.core.metamodel.facets.properties.propertylayout.TypicalLengthFacetForPropertyXml;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.SpecificationLoader;
import org.apache.isis.core.metamodel.spec.SpecificationLoaderAware;
import org.apache.isis.core.metamodel.spec.feature.Contributed;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.metamodel.spec.feature.ObjectMember;
import org.apache.isis.core.metamodel.spec.feature.OneToManyAssociation;
import org.apache.isis.core.metamodel.spec.feature.OneToOneAssociation;

public abstract class GridNormalizerServiceAbstract<G extends Grid>
        implements GridNormalizerService<G>, SpecificationLoaderAware {

    private static final Logger LOG = LoggerFactory.getLogger(GridNormalizerServiceAbstract.class);

    private final Class<G> gridImplementation;
    private final String tns;
    private final String schemaLocation;

    public GridNormalizerServiceAbstract(
            final Class<G> gridImplementation,
            final String tns,
            final String schemaLocation) {
        this.gridImplementation = gridImplementation;
        this.tns = tns;
        this.schemaLocation = schemaLocation;
    }

    @Programmatic
    @Override
    public Class<G> gridImplementation() {
        return gridImplementation;
    }

    @Programmatic
    @Override
    public String tns() {
        return tns;
    }

    @Programmatic
    @Override
    public String schemaLocation() {
        return schemaLocation;
    }

    @Programmatic
    @Override
    public void normalize(final G grid, final Class<?> domainClass) {

        if(!gridImplementation.isAssignableFrom(grid.getClass())) {
            // ignore any other grid implementations
            return;
        }
        final ObjectSpecification objectSpec = specificationLookup.loadSpecification(domainClass);

        final Map<String, OneToOneAssociation> oneToOneAssociationById =
                ObjectMember.Util.mapById(getOneToOneAssociations(objectSpec));
        final Map<String, OneToManyAssociation> oneToManyAssociationById =
                ObjectMember.Util.mapById(getOneToManyAssociations(objectSpec));
        final Map<String, ObjectAction> objectActionById =
                ObjectMember.Util.mapById(objectSpec.getObjectActions(Contributed.INCLUDED));

        final boolean valid =
                validateAndDerive(
                    grid, oneToOneAssociationById, oneToManyAssociationById, objectActionById, objectSpec);
        if (valid) {
            overwrite(grid, oneToOneAssociationById, oneToManyAssociationById, objectActionById);
            if(LOG.isDebugEnabled()) {
                LOG.debug("Grid:\n\n" + jaxbService.toXml(grid) + "\n\n");
            }
        } else {
            container.warnUser("Grid metadata errors; check the error log");
            LOG.error("Grid metadata errors:\n\n" + jaxbService.toXml(grid) + "\n\n");
        }
    }

    @Programmatic
    @Override
    public void complete(final G grid, final Class<?> domainClass) {
        // TODO: do some different logic here...
        normalize(grid, domainClass);
    }

    @Programmatic
    @Override
    public void minimal(final G grid, final Class<?> domainClass) {
        // TODO: do some different logic here...
        normalize(grid, domainClass);
    }
    /**
     * Ensures that all object members (properties, collections and actions) are in the metadata.
     */
    protected abstract boolean validateAndDerive(
            final Grid grid,
            final Map<String, OneToOneAssociation> oneToOneAssociationById,
            final Map<String, OneToManyAssociation> oneToManyAssociationById,
            final Map<String, ObjectAction> objectActionById,
            final ObjectSpecification objectSpec);

    /**
     * Overwrites (replaces) any existing facets in the metamodel with info taken from the grid.
     *
     * <p>
     *     This code uses {@link FacetUtil#addOrReplaceFacet(Facet)} because the layout might be changed multiple times.
     * </p>
     */
    protected void overwrite(
            final G fcGrid,
            final Map<String, OneToOneAssociation> oneToOneAssociationById,
            final Map<String, OneToManyAssociation> oneToManyAssociationById,
            final Map<String, ObjectAction> objectActionById) {

        final Map<String, int[]> propertySequenceByGroup = Maps.newHashMap();
        fcGrid.visit(new FCGrid.VisitorAdapter() {
            private int collectionSequence = 1;

            private int actionDomainObjectSequence = 1;
            private int actionPropertyGroupSequence = 1;
            private int actionPropertySequence = 1;
            private int actionCollectionSequence = 1;

            @Override
            public void visit(final ActionLayoutData actionLayoutData) {
                final ActionLayoutDataOwner actionLayoutDataOwner = actionLayoutData.getOwner();
                final ObjectAction objectAction = objectActionById.get(actionLayoutData.getId());
                if(objectAction == null) {
                    return;
                }

                final String memberOrderName;
                final int memberOrderSequence;
                if(actionLayoutDataOwner instanceof FieldSet) {
                    final FieldSet fieldSet = (FieldSet) actionLayoutDataOwner;
                    final List<PropertyLayoutData> properties = fieldSet.getProperties();
                    final PropertyLayoutData propertyLayoutData = properties.get(0); // any will do
                    memberOrderName = propertyLayoutData.getId();
                    memberOrderSequence = actionPropertyGroupSequence++;
                } else if(actionLayoutDataOwner instanceof PropertyLayoutData) {
                    final PropertyLayoutData propertyLayoutData = (PropertyLayoutData) actionLayoutDataOwner;
                    memberOrderName = propertyLayoutData.getId();
                    memberOrderSequence = actionPropertySequence++;
                } else if(actionLayoutDataOwner instanceof CollectionLayoutData) {
                    final CollectionLayoutData collectionLayoutData = (CollectionLayoutData) actionLayoutDataOwner;
                    memberOrderName = collectionLayoutData.getId();
                    memberOrderSequence = actionCollectionSequence++;
                } else {
                    // means' don't add: any existing metadata should be preserved
                    memberOrderName = null;
                    memberOrderSequence = actionDomainObjectSequence++;
                }
                if(memberOrderName != null) {
                    FacetUtil.addOrReplaceFacet(
                            new MemberOrderFacetXml(memberOrderName, "" + memberOrderSequence, translationService,
                                    objectAction));
                }

                // fix up the action position if required
                if(actionLayoutDataOwner instanceof FieldSet) {
                    if(actionLayoutData.getPosition() == null ||
                            actionLayoutData.getPosition() == org.apache.isis.applib.annotation.ActionLayout.Position.BELOW ||
                            actionLayoutData.getPosition() == org.apache.isis.applib.annotation.ActionLayout.Position.RIGHT) {
                        actionLayoutData.setPosition(org.apache.isis.applib.annotation.ActionLayout.Position.PANEL);
                    }
                } else if(actionLayoutDataOwner instanceof PropertyLayoutData) {
                    if(actionLayoutData.getPosition() == null ||
                            actionLayoutData.getPosition() == org.apache.isis.applib.annotation.ActionLayout.Position.PANEL_DROPDOWN ||
                            actionLayoutData.getPosition() == org.apache.isis.applib.annotation.ActionLayout.Position.PANEL) {
                        actionLayoutData.setPosition(org.apache.isis.applib.annotation.ActionLayout.Position.BELOW);
                    }
                } else {
                    // doesn't do anything for DomainObject or Collection
                    actionLayoutData.setPosition(null);
                }

                FacetUtil.addOrReplaceFacet(ActionPositionFacetForActionXml.create(actionLayoutData, objectAction));
                FacetUtil.addOrReplaceFacet(BookmarkPolicyFacetForActionXml.create(actionLayoutData, objectAction));
                FacetUtil.addOrReplaceFacet(CssClassFacetForActionXml.create(actionLayoutData, objectAction));
                FacetUtil.addOrReplaceFacet(CssClassFaFacetForActionXml.create(actionLayoutData, objectAction));
                FacetUtil.addOrReplaceFacet(DescribedAsFacetForActionXml.create(actionLayoutData, objectAction));
                FacetUtil.addOrReplaceFacet(HiddenFacetForActionLayoutXml.create(actionLayoutData, objectAction));
                FacetUtil.addOrReplaceFacet(NamedFacetForActionXml.create(actionLayoutData, objectAction));
            }

            @Override
            public void visit(final PropertyLayoutData propertyLayoutData) {
                final OneToOneAssociation oneToOneAssociation = oneToOneAssociationById.get(propertyLayoutData.getId());
                if(oneToOneAssociation == null) {
                    return;
                }

                FacetUtil.addOrReplaceFacet(CssClassFacetForPropertyXml.create(propertyLayoutData, oneToOneAssociation));
                FacetUtil.addOrReplaceFacet(DescribedAsFacetForPropertyXml.create(propertyLayoutData, oneToOneAssociation));
                FacetUtil.addOrReplaceFacet(HiddenFacetForPropertyXml.create(propertyLayoutData, oneToOneAssociation));
                FacetUtil.addOrReplaceFacet(LabelAtFacetForPropertyXml.create(propertyLayoutData, oneToOneAssociation));
                FacetUtil.addOrReplaceFacet(MultiLineFacetForPropertyXml.create(propertyLayoutData, oneToOneAssociation));
                FacetUtil.addOrReplaceFacet(NamedFacetForPropertyXml.create(propertyLayoutData, oneToOneAssociation));
                FacetUtil.addOrReplaceFacet(
                        RenderedAdjustedFacetForPropertyXml.create(propertyLayoutData, oneToOneAssociation));
                FacetUtil.addOrReplaceFacet(TypicalLengthFacetForPropertyXml.create(propertyLayoutData, oneToOneAssociation));

                // @MemberOrder#name based on owning property group, @MemberOrder#sequence monotonically increasing
                final FieldSet fieldSet = propertyLayoutData.getOwner();
                final String groupName = fieldSet.getName();
                final String sequence = nextInSequenceFor(groupName, propertySequenceByGroup);
                FacetUtil.addOrReplaceFacet(
                        new MemberOrderFacetXml(groupName, sequence, translationService, oneToOneAssociation));
            }

            @Override
            public void visit(final CollectionLayoutData collectionLayoutData) {
                final OneToManyAssociation oneToManyAssociation = oneToManyAssociationById.get(collectionLayoutData.getId());
                if(oneToManyAssociation == null) {
                    return;
                }

                FacetUtil.addOrReplaceFacet(CssClassFacetForCollectionXml.create(collectionLayoutData, oneToManyAssociation));
                FacetUtil.addOrReplaceFacet(
                        DefaultViewFacetForCollectionXml.create(collectionLayoutData, oneToManyAssociation));
                FacetUtil.addOrReplaceFacet(
                        DescribedAsFacetForCollectionXml.create(collectionLayoutData, oneToManyAssociation));
                FacetUtil.addOrReplaceFacet(HiddenFacetForCollectionXml.create(collectionLayoutData, oneToManyAssociation));
                FacetUtil.addOrReplaceFacet(NamedFacetForCollectionXml.create(collectionLayoutData, oneToManyAssociation));
                FacetUtil.addOrReplaceFacet(PagedFacetForCollectionXml.create(collectionLayoutData, oneToManyAssociation));
                FacetUtil.addOrReplaceFacet(SortedByFacetForCollectionXml.create(collectionLayoutData, oneToManyAssociation));

                // @MemberOrder#name based on the collection's id (so that each has a single "member group")
                final String groupName = collectionLayoutData.getId();
                final String sequence = "" + collectionSequence++;
                FacetUtil.addOrReplaceFacet(
                        new MemberOrderFacetXml(groupName, sequence, translationService, oneToManyAssociation));

                // if there is only a single column and no other contents, then copy the collection Id onto the tab'
                final MemberRegionOwner memberRegionOwner = collectionLayoutData.getOwner();
                if(memberRegionOwner instanceof FCColumn) {
                    final FCColumn FCColumn = (FCColumn) memberRegionOwner;
                    final FCColumnOwner holder = FCColumn.getOwner();
                    if(holder instanceof FCTab) {
                        final FCTab FCTab = (FCTab) holder;
                        if(FCTab.getContents().size() == 1 && Strings.isNullOrEmpty(FCTab.getName()) ) {
                            final String collectionName = oneToManyAssociation.getName();
                            FCTab.setName(collectionName);
                        }
                    }
                }
            }
        });
    }


    protected static List<OneToOneAssociation> getOneToOneAssociations(final ObjectSpecification objectSpec) {
        List associations = objectSpec.getAssociations(Contributed.INCLUDED, ObjectAssociation.Filters.PROPERTIES);
        return associations;
    }
    protected  static List<OneToManyAssociation> getOneToManyAssociations(final ObjectSpecification objectSpec) {
        List associations = objectSpec.getAssociations(Contributed.INCLUDED, ObjectAssociation.Filters.COLLECTIONS);
        return associations;
    }



    protected static class Tuple<T> {
        public final T first;
        public final T second;
        private Tuple(final T first, final T second) {
            this.first = first;
            this.second = second;
        }
        public static <T> Tuple<T> of(final T first, final T second) {
            return new Tuple<>(first, second);
        }
    }


    /**
     * Returns a 2-element tuple of [first-second, second-first]
     */
    protected static <T> Tuple<List<T>> surplusAndMissing(final Collection<T> first, final Collection<T> second){
        final List<T> firstNotSecond = Lists.newArrayList(first);
        firstNotSecond.removeAll(second);
        final List<T> secondNotFirst = Lists.newArrayList(second);
        secondNotFirst.removeAll(first);
        return Tuple.of(firstNotSecond, secondNotFirst);
    }


    protected static String nextInSequenceFor(
            final String key, final Map<String, int[]> seqByKey) {
        synchronized (seqByKey) {
            int[] holder = seqByKey.get(key);
            if(holder == null) {
                holder = new int[]{0};
                seqByKey.put(key, holder);
            }
            holder[0]++;
            return ""+holder[0];
        }
    }


    SpecificationLoader specificationLookup;

    public void setSpecificationLoader(final SpecificationLoader specificationLookup) {
        this.specificationLookup = specificationLookup;
    }

    @Inject
    TranslationService translationService;

    @Inject
    JaxbService jaxbService;

    @Inject
    DomainObjectContainer container;


}
