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
import io.swagger.config.SwaggerConfig;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;

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
	 * Dropwizard initialize method callback
	 */
	public abstract void initializeService(Bootstrap<T> bootstrap);

	/**
	 * Dropwizard run method callback
	 */
    public abstract void runService(T configuration, Environment environment) throws Exception;

    /**
     * Callback to configure swagger.
     */
	public abstract void setupSwagger(BeanConfig beanConfig);
	
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

		// display swagger resources at /swagger uri 
		environment.jersey().register(new ApiListingResource());

		// configure swagger environment
		BeanConfig config = new BeanConfig();
		setupSwagger(config);

	  	// redirect calls to base path to
    	// environment.jersey().register(new RedirectResource("/swagger-ui/index.html?url=/swagger"));
		environment.servlets().addServlet("redirect", 
				new DynamicRedirectServlet("/swagger-ui/index.html?url=/swagger"))
				.addMapping("", "/", "/swagger-ui", "/swagger-ui/");

	}

	
}
