
local $SIG{HUP} = sub {
	print "HUP!";
	sleep(5);
	exit(0);
};

local $SIG{TERM} = sub {
	print "TERM!";
	sleep(5);
	exit(0);
};

local $SIG{INT} = sub {
	print "INT!";
	sleep(5);
	exit(0);
};

$| = 1;

while (<>) {
	chomp $_;
	last if $_ eq "stop";
	print $_, $_, "\n";
}

print "stopped", "\n";
