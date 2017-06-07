var command_history_default = [];
var command_default = "/help";
var page_title = "Web Console";
var application_name = "mirrg.application.service.pwi";
var buttons = [ {
	value : "/help",
	action : function(send) {
		send("/help");
	}
}, {
	value : "Restart On",
	action : function(send) {
		send("/set restart true");
	}
}, {
	value : "Restart Off",
	action : function(send) {
		send("/set restart false");
	}
} ];
