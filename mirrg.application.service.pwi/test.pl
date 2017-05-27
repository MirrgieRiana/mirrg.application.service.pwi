
$| = 1;

while (<>) {
	chomp $_;
	last if $_ eq "stop";
	print $_, $_, "\n";
}

print "stopped", "\n";
