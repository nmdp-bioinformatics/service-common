/*

    service-common  Common libraries and utilities for services modules.
    Copyright (c) 2014-2015 National Marrow Donor Program (NMDP)
    
    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.
    
    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.
    
    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.
    
    > http://www.gnu.org/licenses/lgpl.html

*/

package org.nmdp.service.common.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;

/**
 * Configuration module.
 */
public class ConfigurationModule extends AbstractModule {

	final List<Object> config;
	final Set<Class<? extends Annotation>> annos;
	Logger log = LoggerFactory.getLogger(getClass());
	Stack<Object> seen = new Stack<Object>();

    /**
     * Create a new configuration module with the specified configuration objects.
     *
     * @param config variable number of configuration objects, must not be null
     */
	public ConfigurationModule(Object... config) {
            checkNotNull(config);
		this.config = Collections.unmodifiableList(Arrays.asList(config));
		this.annos = Collections.unmodifiableSet(findConfigAnnotations(config));
	}

	@Override
	protected void configure() {
		for (Object o : config) {
			try {
				bindObjectConfig(o);
			} catch (Exception e) {
				binder().addError(e);
			}
		}
	}
	
	private Set<Class<? extends Annotation>> findConfigAnnotations(Object... config) {
		final Set<Class<? extends Annotation>> annos = new HashSet<Class<? extends Annotation>>();
		for (Object o : config) {
			if (o instanceof Class) {
				Class<?> clazz = (Class<?>)o;
				if (clazz.isAnnotation() && clazz.getAnnotation(BindingAnnotation.class) != null) {
					annos.add((Class<? extends Annotation>) clazz);
				}
				annos.addAll(findConfigAnnotations(clazz.getDeclaredClasses()));
			}
		}
		return annos;
	}
	
	
	private static final Set<Class<? extends Serializable>> stopClasses = new HashSet<>(Arrays.asList(
			Boolean.class, Short.class, Integer.class, Long.class, 
			Float.class, Double.class, String.class, Class.class));

	private void bindObjectConfig(Object value, Annotation... bindAnnos) throws Exception {
		if (null == value || value instanceof Class || seen.contains(value)) return;
		seen.push(value);
		log.trace("stack size: " + seen.size());
		try {
			Class clazz = value.getClass();
			if (null != bindAnnos && bindAnnos.length > 0) {
				Set<Class<? extends Annotation>> bindAnnoClasses = FluentIterable.from(Arrays.asList(bindAnnos))
						.transform(new Function<Annotation, Class<? extends Annotation>>() {
							@Override public Class<? extends Annotation> apply(Annotation anno) {
								return anno.annotationType();
							}})
						.toSet();
				SetView<Class<? extends Annotation>> matchedAnnos = Sets.intersection(annos, Sets.newHashSet(bindAnnoClasses));
				for (Class<? extends Annotation> anno : matchedAnnos) {
					log.info("binding property " + anno.getSimpleName() + ": " +  value);
					bind(clazz).annotatedWith(anno).toInstance(clazz.cast(value));
				}
			}
			if (stopClasses.contains(value.getClass())) return;
			bindBeanProperties(value);
			bindJavaFields(value);
		} finally {
			seen.pop();
		}
	}
	
	private void bindBeanProperties(Object o) throws Exception {
		// bind config in bean properties
		BeanInfo cbi = Introspector.getBeanInfo(o.getClass(), Object.class);
		PropertyDescriptor[] pda = cbi.getPropertyDescriptors();
		if (null != pda) {
			for (PropertyDescriptor pd : pda) {
				Class pt = pd.getPropertyType();
				Method pr = pd.getReadMethod(); 
				if (null == pr) continue;
				Object pv = pr.invoke(o, null);
				if (null != pv) {
					Annotation[] pas = pr.getAnnotations();
					log.trace("scanning bean property: " + pd.getName() + " (" + pd.getPropertyType() + ")");
					bindObjectConfig(pv, pas);
				}
			}
		}
	}

	private void bindJavaFields(Object o) throws Exception {
		Field[] pfa = o.getClass().getDeclaredFields(); // no inherited types considered
		for (Field pf : pfa) {
			Object pv = null;
			try { pv = pf.get(o); } catch (Exception e) {}
			if (null != pv) {
				Annotation[] pas = pf.getAnnotations();
				log.trace("scanning java field: " + pf.getName() + " (" + pf.getType() + ")");
				bindObjectConfig(pv, pas);
			}
		}
	}
	
	private Class<? extends Annotation>[] annoToClass(Annotation... anno) {
		Class<? extends Annotation>[] annoClasses = new Class[anno.length]; 
		int i = 0;
		for (Annotation a : anno) {
			annoClasses[i++] = a.annotationType();
		}
		return annoClasses;
	}
	
}
