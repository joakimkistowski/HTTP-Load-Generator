--[[
	Gets called at the beginning of each "call cycle", perform as much work as possible here.
	Initialize all global variables here.
	Note that math.random is already initialized using a fixed seed (5) for reproducibility.
--]]
function onCycle()
	userid = 1 + math.random(20000)
	prefix = "http://localhost/"
	displayamount = math.random(10)
	calls = {
	"index.html",
	"dslogin.php?username=user".. userid.. "&password=password",
	"dsbrowse.php?customerid=".. userid,
	"dsbrowse.php?browsetype=title&browse_title=ACADEMY&browse_actor=&browse_category=1&limit_num="..
		displayamount.. "&customerid=".. userid,
	"dsbrowse.php?selected_item%5B%5D=585&selected_item%5B%5D=38&customerid=".. userid
	}
end

--[[
	Gets called with ever increasing callnums for each http call until it returns nil.
	Once it returns nil, onCycle() is called again and callnum is reset to 1 (Lua convention).
	
	Here, you can use our HTML helper functions for conditional calls on returned texts (usually HTML, thus the name).
	We offer:
	- html.getMatches( regex )
		Returns all lines in the returned text stream that match a provided regex.
	- html.extractMatches( prefixRegex, postfixRegex )
		Returns all matches that are preceeded by a prefixRegex match and followed by a postfixRegex match.
		The regexes must have one unique match for each line in which they apply.
	- html.extractMatches( prefixRegex, matchingRegex, postfixRegex )
		Variant of extractMatches with a matching regex defining the string that is to be extracted.
--]]
function onCall(callnum)
	if callnum == 4 then
		local categorycount = #(html.getMatches("<OPTION VALUE=\\d+>\\D+</OPTION>"))
		local categorynumber = math.random(categorycount)
		return prefix.. "dsbrowse.php?browse_title=&browse_actor=&browsetype=category&browse_category="..
			categorynumber.. "&limit_num=".. displayamount.. "&customerid=".. userid
	elseif callnum == 5 then
		local dvdids = html.extractMatches("<TD><INPUT NAME=selected_item.. TYPE=CHECKBOX VALUE=", "></TD>")
		local itemID = dvdids[math.random(#dvdids)]
		return prefix.."dsbrowse.php?selected_item%5B%5D=".. itemID.. "&selected_item%5B%5D=38&customerid=".. userid
	elseif calls[callnum] == nil then
		return nil
	else
		return prefix..calls[callnum]
	end
end
