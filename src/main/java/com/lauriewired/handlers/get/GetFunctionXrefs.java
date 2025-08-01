package com.lauriewired.handlers.get;

import com.lauriewired.handlers.Handler;
import com.sun.net.httpserver.HttpExchange;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Program;
import ghidra.program.model.symbol.RefType;
import ghidra.program.model.symbol.Reference;
import ghidra.program.model.symbol.ReferenceIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.lauriewired.util.ParseUtils.*;
import static ghidra.program.util.GhidraProgramUtilities.getCurrentProgram;

/**
 * Handler to get all references to a specific function by name.
 * Expects query parameters: name, offset, limit
 */
public final class GetFunctionXrefs extends Handler {
	public GetFunctionXrefs(PluginTool tool) {
		super(tool, "/function_xrefs");
	}

	/**
	 * Handles the HTTP request to get function cross-references.
	 * Expects query parameters: name, offset, limit
	 * 
	 * @param exchange the HTTP exchange containing the request
	 * @throws Exception if an error occurs while processing the request
	 */
	@Override
	public void handle(HttpExchange exchange) throws Exception {
		Map<String, String> qparams = parseQueryParams(exchange);
		String name = qparams.get("name");
		int offset = parseIntOrDefault(qparams.get("offset"), 0);
		int limit = parseIntOrDefault(qparams.get("limit"), 100);
		sendResponse(exchange, getFunctionXrefs(name, offset, limit));
	}

	/**
	 * Retrieves cross-references to a function by its name.
	 * 
	 * @param functionName the name of the function to find references for
	 * @param offset       the starting index for pagination
	 * @param limit        the maximum number of results to return
	 * @return a string containing the references or an error message
	 */
	private String getFunctionXrefs(String functionName, int offset, int limit) {
		Program program = getCurrentProgram(tool);
		if (program == null)
			return "No program loaded";
		if (functionName == null || functionName.isEmpty())
			return "Function name is required";

		try {
			List<String> refs = new ArrayList<>();
			FunctionManager funcManager = program.getFunctionManager();
			for (Function function : funcManager.getFunctions(true)) {
				if (function.getName().equals(functionName)) {
					Address entryPoint = function.getEntryPoint();
					ReferenceIterator refIter = program.getReferenceManager().getReferencesTo(entryPoint);

					while (refIter.hasNext()) {
						Reference ref = refIter.next();
						Address fromAddr = ref.getFromAddress();
						RefType refType = ref.getReferenceType();

						Function fromFunc = funcManager.getFunctionContaining(fromAddr);
						String funcInfo = (fromFunc != null) ? " in " + fromFunc.getName() : "";

						refs.add(String.format("From %s%s [%s]", fromAddr, funcInfo, refType.getName()));
					}
				}
			}

			if (refs.isEmpty()) {
				return "No references found to function: " + functionName;
			}

			return paginateList(refs, offset, limit);
		} catch (Exception e) {
			return "Error getting function references: " + e.getMessage();
		}
	}
}
