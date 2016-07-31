package com.tools;

public class CoordinateTransforms {

	// helper variables used internal
	private static double PI 						= 3.141592653589793;	// pi
	private static double DEG2RAD 					= PI/180.0; 			// constant to convert degrees to radians
	private static double RAD2DEG					= 180.0/PI;				// constat to convert rad to deg.
	private static double E2 						= 0.00669437999013;   	// WGS84 e^2 of reference ellipsoid
	private static double EARTH_RADIUS				= 6378137;            	// WGS84 equatorial radius of earth (m)
	private static double GEOD_TOL 					= 1e-7;					// tolerance for ecf-geod conversion
	private static int GEOD_MAX_ITER 				= 50; 					// max interation for ecf-geod conversion

	/**
	 * A class to store local coordinates in meters. (east, north, up)
	 * @author Kyle
	 *
	 */
	public static class Local{
		/** local, east (m) */
		public double east;
		/** local, north (m) */
		public double north;
		/** local, up (m) */
		public double up;

		/**
		 * Initialize object with local coordinate
		 * @param east
		 * @param north
		 * @param up
		 */
		public Local(double east, double north, double up){
			this.east = east;
			this.north = north;
			this.up = up;
		}

		/**
		 * Return east, north, up as a 3 element array, used for matrix multiplies
		 * @return
		 */
		public double[] getArray(){
			double[]out = {
					east,
					north,
					up};
			return out;
		}
	};

	/**
	 * A class to store geodetic coordinates in deg, deg, m
	 * @author Kyle
	 *
	 */
	public static class Geodetic{
		/** latitude WGS84 (deg) */
		public double latitude;
		/** longitude WGS84 (deg) */
		public double longitude;
		/** altitude WGS84 (m) */
		public double altitude;

		/** 
		 * Initialize object with local coordinate
		 * @param latitude
		 * @param longitude
		 * @param altitude
		 */
		public Geodetic(double latitude, double longitude, double altitude){
			this.latitude = latitude;
			this.longitude = longitude;
			this.altitude = altitude;
		}

		/**
		 * Convert geodetic to ecf coordinate
		 * @return ecf coordinates
		 */
		public Ecf ecf(){

			// convert to radians and grab altitude
			double lon = longitude*DEG2RAD;
			double lat = latitude*DEG2RAD;
			double alt = altitude;

			// sin and cos of angles
			double sLon = Math.sin(lon);
			double sLat = Math.sin(lat);
			double cLon = Math.cos(lon);
			double cLat = Math.cos(lat);

			// conversion steps
			double N = EARTH_RADIUS / Math.sqrt(1-E2*sLat*sLat);
			Ecf ecf = new Ecf(cLat*cLon*(N+alt), 
					cLat*sLon*(N+alt), 
					sLat*(N*(1-E2)+alt));

			// return ecf
			return ecf;
		}

		/**
		 * Add a local vector to the geodetic position and return the ecf position
		 * @param local
		 * @return the new ecf position
		 */
		public Ecf addLocalVector2Ecf(Local local){

			// altered lat and lon
			double lat2 = -(90 - latitude)*DEG2RAD;
			double lon2 = -(90 + longitude)*DEG2RAD;

			// sin and cos of angles
			double sLon = Math.sin(lon2);
			double sLat = Math.sin(lat2);
			double cLon = Math.cos(lon2);
			double cLat = Math.cos(lat2);

			// grab ecf position
			Ecf ecf = this.ecf();

			// transformation matrixes
			double R1[][] = {
					{1,	0, 		0},
					{0, 	cLat, 	-sLat},
					{0, 	sLat, 	cLat}
			};
			double R3[][] = {
					{cLon,	-sLon, 	0},
					{sLon, 	cLon, 	0},
					{0, 		0, 		1}
			};

			// forward transformation, ecf to local
			double R[][] = com.tools.MathTools.matrixMultiply(R1, R3);

			// we want reverse transform of local to ecf, so transpose
			R = com.tools.MathTools.transposeMatrix(R);
			//return ecf;

			// multiple rotation matrix by local position and add to current position
			ecf = ecf.add(new Ecf(com.tools.MathTools.matrixMultiply(R, local.getArray())));
			
			return ecf;
		}
	
		/**
		 * Add a local vector to the geodetic position and return the geodetic position
		 * @param local
		 * @return the new geodetic position
		 */
		public Geodetic addLocalVector2Geod(Local local){
			Ecf tmp = addLocalVector2Ecf(local);
			return tmp.geodetic();
		}
	}

	/**
	 * A class to store ecf coordinates in m,m,m
	 * @author Kyle
	 *
	 */
	public static class Ecf{
		/** x (m) */
		public double x;
		/** y (m) */
		public double y;
		/** z  (m) */
		public double z;

		/** 
		 * Initialize object with local coordinate
		 * @param x
		 * @param y
		 * @param z
		 */
		public Ecf(double x, double y, double z){
			this.x = x;
			this.y = y;
			this.z = z;
		}

		/**
		 * Input the x, y, z coordinates of ecf as a 3 element array in meters.
		 * @param input (x, y, z) array of ecf coordinates in meters.
		 */
		public Ecf(double[] input){
			// error check inputs
			if (input == null || input.length != 3)
				throw new IllegalArgumentException("inputs into Ecf must not be null and be length==3");

			// assign values
			x = input[0];
			y = input[1];
			z = input[2];
		}
		
		/**
		 * Add two ecf positions together and return the result
		 * @param ecf2
		 * @return
		 */
		public Ecf add(Ecf ecf2){
			return new Ecf(x+ecf2.x, y+ecf2.y, z+ecf2.z);
		}
		
		/**
		 * Convert ecf position to WGS84 geodetic and return
		 * @return geodetic object of ecf coordinates
		 */
		public Geodetic geodetic(){
			
			// calculate rho and long
			double rho = Math.sqrt(x*x + y*y);
			double lon = Math.atan2(y, x);
			double lat = 0;
			
			// Initialize to spherical values
			if (rho == 0){
				if (z >= 0)
					lat = PI/2;
				else
					lat = -PI/2;
			}else
				lat = Math.atan(z/rho);
			
			// Calculate h
			double h = Math.sqrt(x*x + y*y + z*z);
			
			// Iteration loop
			int n = 1;
			boolean conv = false;
			while (!conv && n < GEOD_MAX_ITER){
				
				// various calculations
				double sLat = Math.sin(lat);
				double cLat = Math.cos(lat);
				double u = 1 / Math.sqrt(1-E2*sLat*sLat);
				double N = EARTH_RADIUS*u;
				double v = N + h;
				double w = N*(1-E2) + h;
				double rhoErr = v*cLat - rho;
				double zErr = w*sLat - z;
				
				// check convergence
				if (Math.abs(rhoErr) < GEOD_TOL &&
						Math.abs(zErr) < GEOD_TOL)
					conv = true;
				else{
					double t = Math.abs(w*u*u);
					lat = lat + (sLat*rhoErr - cLat*zErr)/t;
					h = h - cLat*rhoErr - sLat*zErr;
				}
				
				// iterate
				n++;
			}
			
			// outside of loop convert back to degrees
			return new Geodetic(lat*RAD2DEG, lon*RAD2DEG, h);
		}
	}
}