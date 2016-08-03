package org.nmdp.service.common.dropwizard;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DynamicRedirectServlet extends HttpServlet {

	private static final long serialVersionUID = -3078475205397370599L;

	private String uri;

	public DynamicRedirectServlet(String uri) {
		this.uri = uri;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.sendRedirect(uri);
//		resp.setStatus(307);
//		resp.addHeader("Location", uri);
//		resp.setContentType("text/plain");
//		PrintWriter writer = resp.getWriter();
//		writer.print("MOVED");
//		writer.flush();
	}

}
