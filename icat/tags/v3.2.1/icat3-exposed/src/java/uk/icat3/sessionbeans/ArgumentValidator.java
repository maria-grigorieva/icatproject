/*
 * ArgumentValidator.java
 *
 * Created on 27 March 2007, 08:21
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package uk.icat3.sessionbeans;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import org.apache.log4j.Logger;

/**
 *
 * @author gjd37
 */
public class ArgumentValidator {
    
    static Logger log = Logger.getLogger(ArgumentValidator.class);
    
    /**
     * Creates a new instance of ArgumentValidator
     */
    public ArgumentValidator() {
    }
    
   // @AroundInvoke
    public Object checkArguments(InvocationContext ctx) throws Exception {
        Object[] args = ctx.getParameters();
        String className = ctx.getTarget().getClass().getName();
        String methodName = ctx.getMethod().getName();
        
        //build up method call
        StringBuilder builder = new StringBuilder();
        
        builder.append(className+"."+methodName+"(");
        
        int i = 1;
        for(Object arg : args){
            if(arg == null){
                throw new IllegalArgumentException("Cannot pass in null arguments to: "+className+":"+methodName);
            }
            if(i == args.length) builder.append(arg+")");
            else builder.append(arg+", ");
            i++;
        }
        
        log.info(builder.toString());
        
        return ctx.proceed();
        
    }
    
}