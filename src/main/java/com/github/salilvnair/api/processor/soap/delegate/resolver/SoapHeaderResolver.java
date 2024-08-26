package com.github.salilvnair.api.processor.soap.delegate.resolver;

import com.github.salilvnair.api.processor.helper.reflection.ReflectionUtil;
import com.github.salilvnair.api.processor.soap.delegate.resolver.core.SoapWebServiceHeader;
import com.github.salilvnair.api.processor.soap.delegate.resolver.reflect.SoapHeader;
import com.github.salilvnair.api.processor.soap.delegate.resolver.reflect.SoapHeaderAttribute;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

@Component
public class SoapHeaderResolver {


    public void resolve(Object source, SOAPHeader soapHeader, Map<String, Object> wsParamsMap) throws SOAPException {
        resolve(source, soapHeader, wsParamsMap, null);
    }

    private void resolve(Object source, SOAPHeader soapHeader, Map<String, Object> wsParamsMap, SOAPElement soapElement) throws SOAPException {
        if(source.getClass().isAnnotationPresent(SoapHeader.class)) {
            SoapHeader classAnnotation =  source.getClass().getAnnotation(SoapHeader.class);
            //find all fields
            // if the fields are String then resolveSimpleSoapHeader
            // if the fields are Object then do recursively above
            SOAPElement nestedElement;
            if(soapElement == null) {
                // this is root element
                soapElement = soapHeader.addChildElement(classAnnotation.name(), classAnnotation.namespace(),classAnnotation.namespaceUri());
                nestedElement = soapElement;
            }
            else {
                nestedElement = soapElement.addChildElement(classAnnotation.name(), classAnnotation.namespace());
                if(!"".equals(classAnnotation.qNamespace()) && !"".equals(classAnnotation.qNamespaceUri())) {
                    nestedElement.addAttribute(new QName("xmlns:"+classAnnotation.qNamespace()), classAnnotation.qNamespaceUri());
                }
            }
            Set<Field> fields = ReflectionUtil.findFields(source.getClass());
            for (Field field : fields) {
                if(!field.isAnnotationPresent(SoapHeader.class)) {
                    continue;
                }
                if(field.getType() == String.class && nestedElement != null) {
                    resolve(field, source, soapHeader, wsParamsMap, nestedElement);
                }
                else if (SoapWebServiceHeader.class.isAssignableFrom(field.getType())) {
                    Object fieldValue = ReflectionUtil.findFieldValue(source, field);
                    if(fieldValue != null) {
                        resolve(fieldValue, soapHeader, wsParamsMap, nestedElement);
                    }
                }
            }
        }
    }

    private void resolve(Field field, Object source, SOAPHeader soapHeader, Map<String, Object> wsParamsMap, SOAPElement soapElement) throws SOAPException  {
        SoapHeader fieldAnnotation = field.getAnnotation(SoapHeader.class);
        SOAPElement textNodeElement = soapElement.addChildElement(fieldAnnotation.name(), fieldAnnotation.namespace());
        for (SoapHeaderAttribute soapHeaderAttribute : fieldAnnotation.attributes()) {
            textNodeElement.setAttribute(soapHeaderAttribute.name(), soapHeaderAttribute.value());
        }
        textNodeElement.addTextNode(ReflectionUtil.findFieldValue(source, field));
    }
}
