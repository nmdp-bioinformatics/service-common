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

package org.nmdp.service.common.dropwizard;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;
import com.wordnik.swagger.jaxrs.reader.DefaultJaxrsApiReader;
import com.wordnik.swagger.model.ApiInfo;
import com.wordnik.swagger.reader.ClassReaders;

public abstract class CommonServiceApplication<T extends Configuration> extends Application<T> {

	/**
	 * resource path of swagger UI within the classpath
	 */
	static final String SWAGGER_RESOURCE_PATH = "/swagger-ui";

	/**
	 * default URI to map the swagger path to
	 */
	static final String DEFAULT_SWAGGER_URI = "/doc";

	/**
	 * actual swagger URI to map the swagger path to
	 */
	private final String swaggerUri;

	public CommonServiceApplication() {
		this(DEFAULT_SWAGGER_URI);
	}
	
	public CommonServiceApplication(String swaggerPrefix) {
		super();
		this.swaggerUri  = swaggerPrefix;
	}
	
	/** 
	 * dropwizard initialize method callback
	 */
	public abstract void initializeService(Bootstrap<T> bootstrap);

	/**
	 * dropwizard run method callback
	 */
    public abstract void runService(T configuration, Environment environment) throws Exception;

    /**
     * callback to configure swagger.  setBasePath and setApiPath are overridden.
     */
    public abstract void configureSwagger(SwaggerConfig config);

	@Override
	public final void initialize(Bootstrap<T> bootstrap) {
    	bootstrap.addBundle(new AssetsBundle(SWAGGER_RESOURCE_PATH, swaggerUri, "index.html", "swagger-ui"));
    	initializeService(bootstrap);
	}
	
	@Override
	public final void run(T configuration, Environment environment)
			throws Exception 
	{
		setupSwagger(environment);
		runService(configuration, environment);
	}
  
	private void setupSwagger(Environment environment) {

    	// Swagger Resource
        //environment.addResource(new ApiListingResourceJSON());
        environment.jersey().register(new ApiListingResourceJSON());

        // Swagger providers
        //environment.addResource(new ResourceListingProvider());
		environment.jersey().register(new ApiDeclarationProvider());
		//environment.addResource(new ApiDeclarationProvider());
		environment.jersey().register(new ResourceListingProvider());

        // Swagger Scanner, which finds all the resources for @Api Annotations
        ScannerFactory.setScanner(new DefaultJaxrsScanner());

        // Add the reader, which scans the resources and extracts the resource information
        ClassReaders.setReader(new DefaultJaxrsApiReader());

        // Set the swagger config options
        SwaggerConfig config = ConfigFactory.config();
        configureSwagger(config);
  	  	config.setBasePath(environment.getApplicationContext().getContextPath());
  	  	config.setApiPath("/api-doc");
  	  	
  	  	// redirect calls to base path to 
    	environment.jersey().register(new RedirectResource(swaggerUri));
    }

	
}
