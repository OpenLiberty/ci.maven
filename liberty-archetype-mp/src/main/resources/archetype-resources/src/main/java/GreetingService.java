/*******************************************************************************
 * (c) Copyright IBM Corporation 2017.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package ${package};

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import javax.enterprise.context.RequestScoped;
import org.eclipse.microprofile.metrics.annotation.Timed;

@Path("/hello")
@RequestScoped
public class GreetingService {

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed(name = "sayHelloTime", displayName = "Call duration", description = "Time spent in call")
    public Response sayHello(@PathParam("name") String name) {

        Greeting greeting = new Greeting("Hello", name);
        return Response.ok(greeting).build();
    }

}
