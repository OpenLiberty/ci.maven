/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
package application.rest;

import java.util.Collection;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.TypedQuery;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;

import application.transaction.Transaction;

@ApplicationPath("/")
@Path("/")
@Stateless
public class TransactionRestEndpoint extends Application {

    @PersistenceUnit(unitName = "pu")
    private EntityManagerFactory emf;

    @GET
    @Path("/api/transactions")
    @Produces("application/json")
    public Collection<Transaction> hello(@DefaultValue("0") @QueryParam("startTime") long start,
            @DefaultValue("" + Long.MAX_VALUE) @QueryParam("endTime") long end) {

        EntityManager em = emf.createEntityManager();
        TypedQuery<Transaction> q = emf.createEntityManager().createQuery(
                "SELECT transaction FROM Transaction transaction WHERE transaction.time >= :startTime AND transaction.time <= :endTime",
                Transaction.class);
        q.setParameter("startTime", start);
        q.setParameter("endTime", end);
        Collection<Transaction> result = q.getResultList();
        em.close();
        return result;
    }

}