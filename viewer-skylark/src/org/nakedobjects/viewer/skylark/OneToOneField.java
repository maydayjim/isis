package org.nakedobjects.viewer.skylark;

import org.nakedobjects.object.Lookup;
import org.nakedobjects.object.NakedObject;
import org.nakedobjects.object.NakedObjectSpecification;
import org.nakedobjects.object.NakedObjectSpecificationLoader;
import org.nakedobjects.object.reflect.OneToOneAssociationSpecification;
import org.nakedobjects.viewer.skylark.basic.ObjectOption;
import org.nakedobjects.viewer.skylark.basic.RemoveOneToOneAssociationOption;


public class OneToOneField extends ObjectField implements ObjectContent {
    private static final UserAction REMOVE_ASSOCIATION = new RemoveOneToOneAssociationOption();
    private final NakedObject object;

    public OneToOneField(NakedObject parent, NakedObject object, OneToOneAssociationSpecification association) {
        super(parent, association);
        this.object = object;
    }

    public String debugDetails() {
        return super.debugDetails() + "  object:" + object + "\n";
    }

    public NakedObject getObject() {
        return object;
    }

    // TODO move the code using this method (in RemoveOneToOneAssociation) to this class and call from the other
    public OneToOneAssociationSpecification getOneToOneAssociation() {
        return (OneToOneAssociationSpecification) getField();
    }

    public void menuOptions(MenuOptionSet options) {
        super.menuOptions(options);
        ObjectOption.menuOptions(object, options);
        options.add(MenuOptionSet.OBJECT, REMOVE_ASSOCIATION);
    }

    public String toString() {
        return getObject() + "/" + getField();
    }

    public boolean isLookup() {
        NakedObjectSpecification lookup = NakedObjectSpecificationLoader.getInstance().loadSpecification(Lookup.class);
        return getOneToOneAssociation().getType().isOfType(lookup);
    }
}

/*
 * Naked Objects - a framework that exposes behaviourally complete business
 * objects directly to the user. Copyright (C) 2000 - 2004 Naked Objects Group
 * Ltd
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * The authors can be contacted via www.nakedobjects.org (the registered address
 * of Naked Objects Group is Kingsway House, 123 Goldworth Road, Woking GU21
 * 1NR, UK).
 */