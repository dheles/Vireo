package controllers;

import java.lang.annotation.Annotation;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.tdl.vireo.model.ActionLog;
import org.tdl.vireo.model.EmbargoType;
import org.tdl.vireo.model.Person;
import org.tdl.vireo.model.RoleType;
import org.tdl.vireo.model.NamedSearchFilter;
import org.tdl.vireo.model.Submission;
import org.tdl.vireo.model.SubmissionRepository;
import org.tdl.vireo.search.ActiveSearchFilter;
import org.tdl.vireo.search.SearchDirection;
import org.tdl.vireo.search.SearchFilter;
import org.tdl.vireo.search.SearchOrder;
import org.tdl.vireo.search.SearchResult;
import org.tdl.vireo.state.State;


import play.Logger;
import play.data.binding.As;
import play.modules.spring.Spring;
import play.mvc.Catch;
import play.mvc.Http.Cookie;
import play.mvc.With;

/**
 * The controller to handle the reviewer interface for searching, filtering,
 * display, and managing ETDS (aka the list, view and log tabs)
 * 
 * @author <a href="http://www.scottphillips.com">Scott Phillips</a>
 */
@With(Authentication.class)
public class FilterTab extends AbstractVireoController {

	// Store the cookie and session names in an easy to lookup two dimensional
	// array, so that modifySearch() and modifyFilter() can be easily coded to
	// support both sets of names. This allows you to do:
	// NAMES[SUBMISSION][ACTIVE_FILTER] to get the name of the cookie for the
	// submission list's active filter. Or easily switch that to lookup the same
	// name for the action log.
	public final static String[][] NAMES = {
		{
			"SubmissionFilter",
			"SubmissionDirection",
			"SubmissionOrderBy",
			"SubmissionOffset"
		},
		{
			"ActionLogFilter",
			"ActionLogDirection",
			"ActionLogOrderBy",
			"ActionLogOffset"
		}
	};
	
	// Static index lookups into the NAMES array for name sets.
	public final static int SUBMISSION = 0;
	public final static int ACTION_LOG = 1;
	
	// Static index lookups into the NAMES array for particular names.
	public final static int ACTIVE_FILTER = 0;
	public final static int DIRECTION = 1;
	public final static int ORDERBY = 2;
	public final static int OFFSET = 3;
	
	
	
	/**
	 * List page
	 * 
	 * This controller will run the currently active filter and then display the
	 * results. This method does not change any state, instead the
	 * modifyFilter() and modifySearch() methods will handle those modifications
	 * and then redirect back to this method to display the results.
	 * 
	 */
	@Security(RoleType.REVIEWER)
	public static void list() {
		// Get current parameters
		Person person = context.getPerson();
		
		// Step 1: Update the active filter
		//////////
		
		// Load the acive filter from the cookie
		ActiveSearchFilter activeFilter = Spring.getBeanOfType(ActiveSearchFilter.class);
		Cookie cookie = request.cookies.get(NAMES[SUBMISSION][ACTIVE_FILTER]);
		if (cookie != null && cookie.value != null && cookie.value.trim().length() > 0) {
			try {
				activeFilter.decode(cookie.value);
			} catch (RuntimeException re) {
				Logger.warn(re,"Unable to decode search filter: "+cookie.value);
			}
		}
		
		// Step 2:  Run the current filter search
		//////////
		SearchOrder orderby = SearchOrder.ID;
		if (session.get(NAMES[SUBMISSION][ORDERBY]) != null) 
			orderby = SearchOrder.find(Integer.valueOf(session.get(NAMES[SUBMISSION][ORDERBY])));
		
		SearchDirection direction = SearchDirection.ASCENDING;
		if (session.get(NAMES[SUBMISSION][DIRECTION]) != null) 
			direction = SearchDirection.find(Integer.valueOf(session.get(NAMES[SUBMISSION][DIRECTION])));
		
		Integer offset = 0;
		if (session.get(NAMES[SUBMISSION][OFFSET]) != null)
			offset = Integer.valueOf(session.get(NAMES[SUBMISSION][OFFSET]));
		
		// TODO: Look up the limit based upon the user's preferences.
		Integer limit = 100;
		
		SearchResult<Submission> results = subRepo.filterSearchSubmissions(activeFilter,orderby, direction, offset, limit);
		

		// Step 3: Prepare any variables for display
		//////////
		List<NamedSearchFilter> allFilters = subRepo.findSearchFiltersByCreatorOrPublic(person);
		String nav = "list";
		
		// Get a list of columns to display
		// TODO: Make it dynamic which columns to display.
		SearchOrder[] columns = SearchOrder.values();
		
		// Add all search directions to the view
		for (SearchOrder order2 : SearchOrder.values())
			renderArgs.put(order2.name(), order2);
		
	    // Add ASCENDING and DECENDING to the view
		renderArgs.put(SearchDirection.ASCENDING.name(), SearchDirection.ASCENDING);
		renderArgs.put(SearchDirection.DESCENDING.name(), SearchDirection.DESCENDING);
		
		render(nav, allFilters, activeFilter, results, orderby, columns, direction);
	}
	
	/**
	 * Log page
	 * 
	 * This controller will run the currently active filter and then display the
	 * results. This method does not change any state, instead the
	 * modifyFilter() and modifySearch() methods will handle those modifications
	 * and then redirect back to this method to display the results.
	 */
	@Security(RoleType.REVIEWER)
	public static void log() {
		// Get current parameters
		Person person = context.getPerson();
		
		// Step 1: Update the active filter
		//////////
		
		// Load the acive filter from the cookie
		ActiveSearchFilter activeFilter = Spring.getBeanOfType(ActiveSearchFilter.class);
		Cookie cookie = request.cookies.get(NAMES[ACTION_LOG][ACTIVE_FILTER]);
		if (cookie != null && cookie.value != null && cookie.value.trim().length() > 0) {
			try {
				activeFilter.decode(cookie.value);
			} catch (RuntimeException re) {
				Logger.warn(re,"Unable to decode search filter: "+cookie.value);
			}
		}
		
		// Step 2:  Run the current filter search
		//////////
		SearchOrder orderby = SearchOrder.ID;
		if (session.get(NAMES[ACTION_LOG][ORDERBY]) != null) 
			orderby = SearchOrder.find(Integer.valueOf(session.get(NAMES[ACTION_LOG][ORDERBY])));
		
		SearchDirection direction = SearchDirection.ASCENDING;
		if (session.get(NAMES[ACTION_LOG][DIRECTION]) != null) 
			direction = SearchDirection.find(Integer.valueOf(session.get(NAMES[ACTION_LOG][DIRECTION])));
		
		Integer offset = 0;
		if (session.get(NAMES[ACTION_LOG][OFFSET]) != null)
			offset = Integer.valueOf(session.get(NAMES[ACTION_LOG][OFFSET]));
		
		// TODO: Look up the limit based upon the user's preferences.
		Integer limit = 100;
		
		SearchResult<ActionLog> results = subRepo.filterSearchActionLogs(activeFilter,orderby, direction, offset, limit);
		

		// Step 3: Prepare any variables for display
		//////////
		List<NamedSearchFilter> allFilters = subRepo.findSearchFiltersByCreatorOrPublic(person);
		String nav = "log";
		
		// Get a list of columns to display
		// TODO: Make it dynamic which columns to display.
		SearchOrder[] columns = SearchOrder.values();
		
		// Add all search directions to the view
		for (SearchOrder order2 : SearchOrder.values())
			renderArgs.put(order2.name(), order2);
		
	    // Add ASCENDING and DECENDING to the view
		renderArgs.put(SearchDirection.ASCENDING.name(), SearchDirection.ASCENDING);
		renderArgs.put(SearchDirection.DESCENDING.name(), SearchDirection.DESCENDING);
		
		render(nav, allFilters, activeFilter, results, orderby, columns, direction);
	}
	
	/**
	 * Modify the current search parameters.
	 * 
	 * This includes things like the sort column, direction, and pagination
	 * offset. These parameters are received and updated in the session before
	 * being redirected back to the list() or log() methods.
	 * 
	 * @param nav
	 *            The current mode: list or log.
	 */
	@Security(RoleType.REVIEWER)
	public static void modifySearch(String nav) {
		
		int type = SUBMISSION;
		if ("log".equals(nav))
			type = ACTION_LOG;
				
		String direction = params.get("direction");
		Integer orderby = params.get("orderby",Integer.class);
		Integer offset = params.get("offset", Integer.class);
		
		if (direction != null) {
			// Toggle the current direction.
			if (String.valueOf(SearchDirection.DESCENDING.getId()).equals(session.get(NAMES[type][DIRECTION]))) {
				session.put(NAMES[type][DIRECTION],SearchDirection.ASCENDING.getId());
			} else {
				session.put(NAMES[type][DIRECTION],SearchDirection.DESCENDING.getId());
			}
			session.remove(NAMES[type][OFFSET]);
		}
		
		if (orderby != null && SearchOrder.find(orderby) != null) {
			session.put(NAMES[type][ORDERBY], orderby);
			session.remove(NAMES[type][OFFSET]);
		}
		
		if (offset != null)
			session.put(NAMES[type][OFFSET], offset);
			
		if ("list".equals(nav))
			list();
		if ("log".equals(nav))
			log();
	}
	
	/**
	 * Modify the current active filter.
	 * 
	 * Operations supported are:
	 * 
	 * ADD: Add a new parameter of any type to the currently active filter.
	 * 
	 * REMOVE: Remove a parameter of any type from the currently active filter.
	 * 
	 * SAVE: Save the currently active filter into the database as a named
	 * filter.
	 * 
	 * MANAGE: Permanently remove saved filters from the database.
	 * 
	 * LOAD: Load a saved filter from the database as the currently active
	 * filter.
	 * 
	 * CLEAR: Clear out the currently active filter.
	 * 
	 * @param nav
	 *            The current mode: list or log
	 */
	@Security(RoleType.REVIEWER)
	public static void modifyFilter(String nav) {
		
		Person person = context.getPerson();
		
		int type = SUBMISSION;
		if ("log".equals(nav))
			type = ACTION_LOG;
		
		// Load the active filter from the cookie
		ActiveSearchFilter activeFilter = Spring.getBeanOfType(ActiveSearchFilter.class);
		Cookie cookie = request.cookies.get(NAMES[type][ACTIVE_FILTER]);
		if (cookie != null) {
			try {
				activeFilter.decode(cookie.value);
			} catch (RuntimeException re) {
				Logger.warn(re,"Unable to decode search filter: "+cookie.value);
			}
		}
		
		String action = params.get("action");
		if ("add".equals(action)) {
			// The user is going to modify the existing active filter by adding a new paramater.
			doAddFilterParameter(activeFilter);
		
		} else if ("remove".equals(action)) {
			// The user is going to modify the existing active filter by removing an existing paramater.
			doRemoveFilterParamater(activeFilter);
			
		} else if ("save".equals(action)) {
			String name = params.get("name");
			boolean publicFlag = false;
			if (params.get("public") != null)
				publicFlag = true;
			
			// Check if a filter allready exsits for the name.
			NamedSearchFilter namedFilter = subRepo.findSearchFilterByCreatorAndName(person, name);
			if (namedFilter == null) {
				namedFilter = subRepo.createSearchFilter(person, name);
			}
			
			namedFilter.setPublic(publicFlag);
			activeFilter.copyTo(namedFilter);
			namedFilter.save();
			
		} else if ("manage".equals(action)) {
			String[] removeIds = params.getAll("remove");
			for (String removeId : removeIds) {	
				NamedSearchFilter namedFilter = subRepo.findSearchFilter(Long.valueOf(removeId));
				
				if (namedFilter.getCreator() == person || person.getRole().ordinal() >= RoleType.MANAGER.ordinal())
					namedFilter.delete();
			}			
			
		} else if ("load".equals(action)) {
			Long filterId = params.get("filter",Long.class);
			NamedSearchFilter savedFilter = subRepo.findSearchFilter(filterId);
			
			if (savedFilter.isPublic() || savedFilter.getCreator() == person)
				activeFilter.copyFrom(savedFilter);
			
		} else if ("clear".equals(action)) {
			// Reset the users current filter by clearing it completely.
			activeFilter = Spring.getBeanOfType(ActiveSearchFilter.class);
			
		} else {
			error("Unknown filter modification action");
		}
		
		// Save the active filter to a cookie
		response.setCookie(NAMES[type][ACTIVE_FILTER], activeFilter.encode());
		session.remove("offset");
		
		if ("list".equals(nav))
			list();
		if ("log".equals(nav))
			log();
		
		error("Unknown list modify navigation controll");
	}

	/**
	 * When an error occurs it is very likely related to the current
	 * configuration which is saved in cookies, such as the current filter
	 * search, offset, etc... To prevent the user from being trapped in an
	 * endless loop we will record the error in the logs, then clear the current
	 * state and send them back to the list or log pages with a clean slate.
	 * 
	 * The views know to show the error message indicating that the state has 
	 * been cleared.
	 * 
	 * @param throwable
	 */
	@Catch(RuntimeException.class)
	public static void handleError(Throwable throwable) {
		
		Logger.error(throwable, "Error on the List or Log tab, clearing the users session to recover.");
		
		// When an error occurs, clear the current state so the user is not
		// trapped in an endless loop they can't recover from without
		// clearing their cookies. Then save the error message on the flash
		// and report it to the user on their next page view.
		
		// Clear out everything related to submission
		response.setCookie(NAMES[SUBMISSION][ACTIVE_FILTER],"");

		session.remove(NAMES[SUBMISSION][DIRECTION]);
		session.remove(NAMES[SUBMISSION][ORDERBY]);
		session.remove(NAMES[SUBMISSION][OFFSET]);
		
		// Clear out everything related to action logs
		response.setCookie(NAMES[ACTION_LOG][ACTIVE_FILTER],"");

		session.remove(NAMES[ACTION_LOG][DIRECTION]);
		session.remove(NAMES[ACTION_LOG][ORDERBY]);
		session.remove(NAMES[ACTION_LOG][OFFSET]);
		
		// Store the error so it can be displayed.
		flash.put("error", throwable.getMessage());
		
		// Check for an endless error loop.
		String errorLoop = flash.get("errorLoop");
		flash.put("errorLoop", "maybe");
		
		if (errorLoop == null) {
			// Only redirect if no error loop is detected.
			if ("log".equals(request.actionMethod)) 
				FilterTab.log();
			
			if ("list".equals(request.actionMethod)) 
				FilterTab.list();
			
			if ("log".equals(request.routeArgs.get("nav")))
				FilterTab.log();
			
			if ("list".equals(request.routeArgs.get("nav")))
				FilterTab.list();
		}
	}
	
	
	/**
	 * Internal method to handle modification of the active search filter when
	 * adding new parameters to the filter.
	 * 
	 * This method expects that on the params are "type", and "value". Type will
	 * be a textual representation of the search filter parameter type such as
	 * "text", or "state" etc... While value will be the actual value of the new
	 * parameter to be added.
	 * 
	 * There are a few special case for date range searching: startDate, endDate,
	 *  year, and month.
	 * 
	 * @param activeFilter
	 *            The active filter to modifiy.
	 */
	protected static void doAddFilterParameter(SearchFilter activeFilter) {
		
		String type = params.get("type");
		String value = params.get("value");
		
		if ("text".equals(type)) {
			activeFilter.addSearchText(value);
			
		} else if ("state".equals(type)) {
			activeFilter.addState(value);
			
		} else if ("assignee".equals(type)) {
			if ("null".equals(params.get("value"))) {
				// Unassigned
				activeFilter.addAssignee(null);
			} else {
				// A specific person
				Long personId = params.get("value",Long.class);
				Person person = personRepo.findPerson(personId);
				activeFilter.addAssignee(person);
			}
			
		} else if ("embargo".equals(type)) {
			Long embargoId = params.get("value",Long.class);
			EmbargoType embargo = settingRepo.findEmbargoType(embargoId);
			activeFilter.addEmbargoType(embargo);
			
		} else if ("semester".equals(type)) {
			Integer year = params.get("year",Integer.class);
			Integer month = params.get("month",Integer.class);
			activeFilter.addGraduationSemester(year, month);

		} else if ("degree".equals(type)) {
			activeFilter.addDegree(value);
			
		} else if ("department".equals(type)) {
			activeFilter.addDepartment(value);
			
		} else if ("college".equals(type)) {
			activeFilter.addCollege(value);
		
		} else if ("major".equals(type)) {
			activeFilter.addMajor(value);
		
		} else if ("docType".equals(type)) {
			activeFilter.addDocumentType(value);
		
		} else if ("umi".equals(type)) {
			Boolean release = params.get("value",Boolean.class);
			activeFilter.setUMIRelease(release);
		
		} else if ("range".equals(type)) {
			
			// First handle start date
			Date startDate = params.get("startDate", Date.class);
			if (startDate != null) {
				Calendar start = Calendar.getInstance();
				
				start.setTime(startDate);
				
				// Always set the minimal hour, minute, second so that the start date is inclusive.
				start.set(Calendar.HOUR,start.getActualMinimum(Calendar.HOUR));
				start.set(Calendar.MINUTE,start.getActualMinimum(Calendar.MINUTE));
				start.set(Calendar.SECOND,start.getActualMinimum(Calendar.SECOND));
				
				activeFilter.setSubmissionDateRangeStart(start.getTime());
			}
			
			// Next handle end date
			Date endDate = params.get("endDate", Date.class);
			if (endDate != null) {
				Calendar end = Calendar.getInstance();
				
				end.setTime(endDate);
				
				// Always set the maximal hour, minute, second so that the dates are inclusive.
				end.set(Calendar.HOUR,end.getActualMaximum(Calendar.HOUR));
				end.set(Calendar.MINUTE,end.getActualMaximum(Calendar.MINUTE));
				end.set(Calendar.SECOND,end.getActualMaximum(Calendar.SECOND));
				
				activeFilter.setSubmissionDateRangeEnd(end.getTime());
			}
		} else if ("rangeChoose".equals(type)) {
			
			Integer year = params.get("year",Integer.class);
			Integer month = params.get("month",Integer.class);
			Integer days = params.get("days",Integer.class);
			
			// Generate the start & end dates
			Calendar start = Calendar.getInstance();
			Calendar end = Calendar.getInstance();

			start.clear();
			end.clear();
			
			start.set(Calendar.YEAR, year);
			end.set(Calendar.YEAR, year);
			
			// Check if user has chosen down to the month.
			if (month == null) {
				start.set(Calendar.MONTH, Calendar.JANUARY);
				end.set(Calendar.MONTH, Calendar.DECEMBER);
			} else {
				start.set(Calendar.MONTH, month);
				end.set(Calendar.MONTH, month);
			}
			
			// Check if the user has chosen down to the day range.
			if (days == null) {
				start.set(Calendar.DAY_OF_MONTH,end.getActualMinimum(Calendar.DAY_OF_MONTH));
				end.set(Calendar.DAY_OF_MONTH,end.getActualMaximum(Calendar.DAY_OF_MONTH));
			} else {
				// Three cases:
				// 1 = days 1-10 of the current month
				// 11 = days 11-20 of the current month
				// 21 = days 21-31 (or whatever is the last day) of the current month.
				if (days == 1) {
					start.set(Calendar.DAY_OF_MONTH,1);
					end.set(Calendar.DAY_OF_MONTH,10);
				} else if (days == 11) {
					start.set(Calendar.DAY_OF_MONTH,11);
					end.set(Calendar.DAY_OF_MONTH,20);
				} else if (days == 21) {
					start.set(Calendar.DAY_OF_MONTH,21);
					end.set(Calendar.DAY_OF_MONTH,end.getActualMaximum(Calendar.DAY_OF_MONTH));
				}
			}
			
			// Always set the maximal hour, minute, second so that the dates are inclusive.
			start.set(Calendar.HOUR,end.getActualMinimum(Calendar.HOUR));
			start.set(Calendar.MINUTE,end.getActualMinimum(Calendar.MINUTE));
			start.set(Calendar.SECOND,end.getActualMinimum(Calendar.SECOND));
			end.set(Calendar.HOUR,end.getActualMaximum(Calendar.HOUR));
			end.set(Calendar.MINUTE,end.getActualMaximum(Calendar.MINUTE));
			end.set(Calendar.SECOND,end.getActualMaximum(Calendar.SECOND));
			
			// Set the range
			activeFilter.setSubmissionDateRangeStart(start.getTime());
			activeFilter.setSubmissionDateRangeEnd(end.getTime());			
		} else {
			error("Unable to add an unknown filter paramater.");
		}
	}
	
	/**
	 * Internal method to handle modification of the active search filter when
	 * removing parameters from the filter.
	 * 
	 * This method expects that on the params are "type", and "value". Type will
	 * be a textual representation of the search filter parameter type such as
	 * "text", or "state" etc... While value will be the actual value of the old
	 * parameter to be removed.
	 * 
	 * There are a few special case for date range searching: startDate, endDate,
	 * year, and month..
	 * 
	 * @param activeFilter
	 *            The active filter to modifiy.
	 */
	protected static void doRemoveFilterParamater(SearchFilter activeFilter) {
		
		String type = params.get("type");
		String value = params.get("value");
		
		
		if ("text".equals(type)) {
			activeFilter.removeSearchText(value);
			
		} else if ("state".equals(type)) {
			activeFilter.removeState(value);
			
		} else if ("assignee".equals(type)) {
			if ("null".equals(params.get("value"))) {
				// Unassigned
				activeFilter.removeAssignee(null);
			} else {
				// A specific person
				Long personId = params.get("value",Long.class);
				Person person = personRepo.findPerson(personId);
				activeFilter.removeAssignee(person);
			}
		} else if ("embargo".equals(type)) {
			Long embargoId = params.get("value",Long.class);
			EmbargoType embargo = settingRepo.findEmbargoType(embargoId);
			activeFilter.removeEmbargoType(embargo);
			
		} else if ("semester".equals(type)) {
			Integer year = params.get("year",Integer.class);
			Integer month = params.get("month",Integer.class);
			activeFilter.removeGraduationSemester(year, month);

		} else if ("degree".equals(type)) {
			activeFilter.removeDegree(value);
		
		} else if ("department".equals(type)) {
			activeFilter.removeDepartment(value);
		
		} else if ("college".equals(type)) {
			activeFilter.removeCollege(value);
		
		} else if ("major".equals(type)) {
			activeFilter.removeMajor(value);
		
		} else if ("docType".equals(type)) {
			activeFilter.removeDocumentType(value);

		} else if ("umi".equals(type)) {
			activeFilter.setUMIRelease(null);
			
		} else if ("rangeStart".equals(type)) {
			activeFilter.setSubmissionDateRangeStart(null);
		
		} else if ("rangeEnd".equals(type)) {
			activeFilter.setSubmissionDateRangeEnd(null);

		} else {	
			error("Unable to remove an unknown filter paramater.");
		}
	}
	
	
	
}