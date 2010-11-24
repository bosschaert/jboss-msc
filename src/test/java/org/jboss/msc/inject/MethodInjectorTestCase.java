/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.msc.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jboss.msc.translate.util.TargetWrapper;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;
import org.jboss.msc.value.util.AnotherService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for {@link MethodInjector}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 *
 */
public class MethodInjectorTestCase {
    private static Value<AnotherService> target;
    private static Value<Method> method;
    private List<Value<Boolean>> arguments;

    @BeforeClass
    public static void setUpStatic() throws Exception {
        target = new ImmediateValue<AnotherService>(new AnotherService(1, true, "JBoss"));
        method = new ImmediateValue<Method>(AnotherService.class.getMethod("discoverDefinedBy", boolean.class));
    }

    @Before
    public void setUp() {
        arguments = new ArrayList<Value<Boolean>>();
    }

    @Test
    public void publicMethod() throws Exception {
        arguments.add(new ImmediateValue<Boolean>(false));
        final Injector<String> injector = new MethodInjector<String>(method, target, Values.immediateValue("String"), arguments);
        Values.thisValue().setValue(Values.immediateValue(this));
        Values.injectedValue().setValue(Values.immediateValue("injected"));
        injector.inject("another String");
        assertSame(this, Values.thisValue().getValue());
        assertEquals("injected", Values.injectedValue().getValue());
        assertEquals("JBoss", AnotherService.getLastDiscovered());
        injector.uninject();
        assertSame(this, Values.thisValue().getValue());
        assertEquals("injected", Values.injectedValue().getValue());
        assertEquals("JBoss", AnotherService.getLastDiscovered());
    }

    @Test
    public void publicStaticMethod() throws Exception {
        final Value<Method> method = Values.immediateValue(AnotherService.class.getMethod("discoverDefinedBy"));
        final Injector<Void> injector = new MethodInjector<Void>(method, target, null, arguments);
        Values.thisValue().setValue(Values.immediateValue(this));
        Values.injectedValue().setValue(Values.immediateValue("injected"));
        injector.inject(null);
        assertSame(this, Values.thisValue().getValue());
        assertEquals("injected", Values.injectedValue().getValue());
        assertEquals("JBoss", AnotherService.getLastDiscovered());
        injector.uninject();
        assertSame(this, Values.thisValue().getValue());
        assertEquals("injected", Values.injectedValue().getValue());
        assertEquals("JBoss", AnotherService.getLastDiscovered());
    }

    @Test
    public void publicMethodFromTargetWrapper() throws Exception {
        final TargetWrapper<?> targetWrapper = new TargetWrapper<Object>(new StringBuffer());
        final Value<Method> method = Values.immediateValue(TargetWrapper.class.getMethod("readTarget"));
        final Injector<String> injector = new MethodInjector<String>(method, Values.immediateValue(targetWrapper), Values.immediateValue("target"), arguments);
        Values.thisValue().setValue(Values.immediateValue(this));
        Values.injectedValue().setValue(Values.immediateValue("injected"));
        injector.inject("another target");
        assertSame(this, Values.thisValue().getValue());
        assertEquals("injected", Values.injectedValue().getValue());
        assertEquals("another target", targetWrapper.getTarget());
        injector.uninject();
        assertSame(this, Values.thisValue().getValue());
        assertEquals("injected", Values.injectedValue().getValue());
        assertEquals("target", targetWrapper.getTarget());
    }

    @Test
    public void publicMethodWithExceptionOnUninjection() throws Exception {
        final TargetWrapper<?> targetWrapper = new TargetWrapper<Object>(new StringBuffer());
        final Value<Method> method = Values.immediateValue(TargetWrapper.class.getMethod("readTarget"));
        final Injector<String> injector = new MethodInjector<String>(method, Values.immediateValue(targetWrapper), Values.nullValue(), arguments);
        Values.thisValue().setValue(Values.immediateValue(this));
        Values.injectedValue().setValue(Values.immediateValue("injected"));
        injector.uninject();
        assertSame(this, Values.thisValue().getValue());
        assertEquals("injected", Values.injectedValue().getValue());
        assertNull(targetWrapper.getTarget());
        injector.inject("another target");
        assertSame(this, Values.thisValue().getValue());
        assertEquals("injected", Values.injectedValue().getValue());
        assertEquals("another target", targetWrapper.getTarget());
        injector.uninject();
        assertSame(this, Values.thisValue().getValue());
        assertEquals("injected", Values.injectedValue().getValue());
        assertNull(targetWrapper.getTarget());
    }

    @Test
    public void unaccessibleMethod() throws Exception {
        final Value<Method> unaccessibleMethod = new ImmediateValue<Method>(AnotherService.class.getDeclaredMethod("getDefinedBy"));
        final Injector<String> injector = new MethodInjector<String>(unaccessibleMethod, target, Values.immediateValue(false), arguments);
        try {
            injector.inject("should throw exception");
            fail("InjectionExcpetion expected");
        } catch (InjectionException e) {}
    }

    @Test
    public void methodWithException() throws Exception {
        arguments.add(new ImmediateValue<Boolean>(true));
        final Injector<String> injector = new MethodInjector<String>(method, target, Values.immediateValue(10), arguments);
        try {
            injector.inject("should throw exception again");
            fail("InjectionException expected");
        } catch (InjectionException e) {}
    }

    @Test
    public void nullMethod() throws Exception {
        arguments.add(new ImmediateValue<Boolean>(false));
        final Value<?> injected = Values.immediateValue(new Object());

        Injector<String> injector = new MethodInjector<String>(null, target, injected, arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(method, null, injected, arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(method, target, injected, null);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(null, null, injected, arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(null, target, null, arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(null, target, injected, null);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(method, null, null, arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(method, null, injected, null);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(null, null, null, arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(null, null, injected, null);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(null, null, null, null);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(Values.<Method>nullValue(), target, injected, arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(method, Values.<AnotherService>nullValue(), injected, arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(Values.<Method>nullValue(), Values.<AnotherService>nullValue(), injected, arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(Values.<Method>nullValue(), target, Values.nullValue(), arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(Values.<Method>nullValue(), Values.<AnotherService>nullValue(), Values.nullValue(), arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        arguments.clear();
        arguments.add(Values.<Boolean>nullValue());
        injector = new MethodInjector<String>(method, target, injected, arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(Values.<Method>nullValue(), target, injected, arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(method, Values.<AnotherService>nullValue(), injected, arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(method, target, Values.nullValue(), arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(Values.<Method>nullValue(), Values.<AnotherService>nullValue(), injected, arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(Values.<Method>nullValue(), target, Values.nullValue(), arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new MethodInjector<String>(Values.<Method>nullValue(), Values.<AnotherService>nullValue(), Values.nullValue(), arguments);
        try {
            injector.inject("exception expected");
            fail("InjectionException expected");
        } catch (InjectionException e) {}
    }
}
