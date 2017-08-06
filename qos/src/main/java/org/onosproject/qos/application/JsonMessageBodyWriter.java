/*
 *  Copyright 2016 WIPRO Technologies Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * File Name    : EventListenerManager.java.
 * Author       : Bivas
 * Date         : 16-Jun-2016
 */
package org.onosproject.qos.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Produces("application/json")
public class JsonMessageBodyWriter implements MessageBodyWriter<JSONObject> {

    private ObjectMapper mapper = new ObjectMapper();
    @Override
    public long getSize(JSONObject jsonObject, Class<?> type,
                        Type genericType, Annotation[] annotations, MediaType mediaType) {
        System.out.println("Bivas - 1");
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        System.out.println("Bivas - 2");
        return JSONObject.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(JSONObject jsonObject,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        System.out.println("Bivas - 1");
        mapper.writer().writeValue(entityStream, jsonObject);
        entityStream.flush();
        /*try {
            JSONObject jobj = JSONObject.newInstance(JSONObject.class);
            // serialize the entity myBean to the entity output stream
            jobj.createMarshaller().marshal(jsonObject, entityStream);
        } catch (JAXBException jaxbException) {
            throw new ProcessingException(
                    "Error serializing a JSONObject to the output stream", jaxbException);
        }*/
    }
}
