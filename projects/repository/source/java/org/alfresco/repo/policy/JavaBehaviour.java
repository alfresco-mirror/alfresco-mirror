/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.policy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.alfresco.util.ParameterCheck;


/**
 * Java based Behaviour.
 * 
 * A behavior acts like a delegate (a method pointer).  The pointer is
 * represented by an instance object and method name.
 * 
 * @author David Caruana
 *
 */
public class JavaBehaviour implements Behaviour
{
    // The object instance holding the method
    private Object instance;
    
    // The method name
    private String method;
    
    // Cache of interface proxies (by interface class)
    private Map<Class, Object> proxies = new HashMap<Class, Object>();
    
    // Enable / Disable invocation of behaviour
    private StackThreadLocal disabled = new StackThreadLocal();

    
    /**
     * Construct.
     * 
     * @param instance  the object instance holding the method
     * @param method  the method name
     */
    public JavaBehaviour(Object instance, String method)
    {
        ParameterCheck.mandatory("Instance", instance);
        ParameterCheck.mandatory("Method", method);
        this.instance = instance;
        this.method = method;
    }


    /* (non-Javadoc)
     * @see org.alfresco.repo.policy.Behaviour#getInterface(java.lang.Class)
     */
    public synchronized <T> T getInterface(Class<T> policy)
    {
        ParameterCheck.mandatory("Policy class", policy);
        Object proxy = proxies.get(policy);
        if (proxy == null)
        {
            InvocationHandler handler = getInvocationHandler(instance, method, policy);
            proxy = Proxy.newProxyInstance(policy.getClassLoader(), new Class[]{policy}, handler);
            proxies.put(policy, proxy);
        }
        return (T)proxy;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.policy.Behaviour#disable()
     */
    public void disable()
    {
        Stack<Integer> stack = disabled.get();
        stack.push(hashCode());
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.policy.Behaviour#enable()
     */
    public void enable()
    {
        Stack<Integer> stack = disabled.get();
        if (stack.peek().equals(hashCode()) == false)
        {
            throw new PolicyException("Cannot enable " + this.toString() + " at this time - mismatched with disable calls");
        }
        stack.pop();
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.policy.Behaviour#isEnabled()
     */
    public boolean isEnabled()
    {
        Stack<Integer> stack = disabled.get();
        return stack.search(hashCode()) == -1;
    }
    
    @Override
    public String toString()
    {
        return "Java method[class=" + instance.getClass().getName() + ", method=" + method + "]";
    }

    /**
     * Gets the Invocation Handler.
     * 
     * @param <T>  the policy interface class
     * @param instance  the object instance
     * @param method  the method name
     * @param policyIF  the policy interface class  
     * @return  the invocation handler
     */
    private <T> InvocationHandler getInvocationHandler(Object instance, String method, Class<T> policyIF)
    {
        Method[] policyIFMethods = policyIF.getMethods();
        if (policyIFMethods.length != 1)
        {
            throw new PolicyException("Policy interface " + policyIF.getCanonicalName() + " must have only one method");
        }

        try
        {
            Class instanceClass = instance.getClass();
            Method delegateMethod = instanceClass.getMethod(method, (Class[])policyIFMethods[0].getParameterTypes());
            return new JavaMethodInvocationHandler(this, delegateMethod);
        }
        catch (NoSuchMethodException e)
        {
            throw new PolicyException("Method " + method + " not found or accessible on " + instance.getClass(), e);
        }
    }
    

    /**
     * Stack specific Thread Local
     * 
     * @author David Caruana
     */
    private class StackThreadLocal extends ThreadLocal<Stack<Integer>>
    {
        @Override
        protected Stack<Integer> initialValue()
        {
            return new Stack<Integer>();
        }
    }
    
    
    /**
     * Java Method Invocation Handler
     * 
     * @author David Caruana
     */
    private static class JavaMethodInvocationHandler implements InvocationHandler
    {
        private JavaBehaviour behaviour;
        private Method delegateMethod;
        
        /**
         * Constuct.
         * 
         * @param instance  the object instance holding the method
         * @param delegateMethod  the method to invoke
         */
        private JavaMethodInvocationHandler(JavaBehaviour behaviour, Method delegateMethod)
        {
            this.behaviour = behaviour;
            this.delegateMethod = delegateMethod;
        }

        /* (non-Javadoc)
         * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
         */
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            // Handle Object level methods
            if (method.getName().equals("toString"))
            {
                return toString();
            }
            else if (method.getName().equals("hashCode"))
            {
                return hashCode();
            }
            else if (method.getName().equals("equals"))
            {
                if (Proxy.isProxyClass(args[0].getClass()))
                {
                    return equals(Proxy.getInvocationHandler(args[0]));
                }
                return false;
            }
            
            // Delegate to designated method pointer
            if (behaviour.isEnabled())
            {
                try
                {
                    behaviour.disable();
                    return delegateMethod.invoke(behaviour.instance, args);
                }
                catch (InvocationTargetException e)
                {
                    throw e.getCause();
                }
                finally
                {
                    behaviour.enable();
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this)
            {
                return true;
            }
            else if (obj == null || !(obj instanceof JavaMethodInvocationHandler))
            {
                return false;
            }
            JavaMethodInvocationHandler other = (JavaMethodInvocationHandler)obj;
            return behaviour.instance.equals(other.behaviour.instance) && delegateMethod.equals(other.delegateMethod);
        }

        @Override
        public int hashCode()
        {
            return 37 * behaviour.instance.hashCode() + delegateMethod.hashCode();
        }

        @Override
        public String toString()
        {
            return "JavaBehaviour[instance=" + behaviour.instance.hashCode() + ", method=" + delegateMethod.toString() + "]";
        }
    }
    
}
