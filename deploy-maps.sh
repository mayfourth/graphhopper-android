URL=192.168.0.102
GH=/sdcard/graphhopper/maps/

# if you install sshdroid you can scp your files to your android device

# wget http://mapsforge.googlecode.com/files/berlin.map
# alternatives: http://download.mapsforge.org/maps/
scp -P 2222 berlin.map root@$URL:$GH

# wget http://download.geofabrik.de/osm/europe/germany/berlin.osm.bz2
# bunzip2 berlin.osm.bz2
# cd ../core/
# ./run.sh ../android-example/berlin
# mv graph-berlin.osm/ ../android-example/
# cd ../android-example/
scp -r -P 2222 graph-berlin.osm/ root@$URL:$GH