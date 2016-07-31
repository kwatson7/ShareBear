package com.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * A variety of math tools
 * @author Kyle
 *
 */
public class MathTools {
	/** number to add to an ascii chart to make it the correct int value */
	private static final int CONVERT_CHAR_TO_INT = -48;
	
	/** Take in a number and return the list of divisors of that number
	 * @param number The number which we want to find the divisors
	 * @return An ArrayList <Long> of the divisors of the number
	 */
	public static ArrayList <Long> divisors(long number){

		// the array to return
		ArrayList <Long> result = new ArrayList<Long>();

		// determine if even
		int step;
		if (!isEven(number))
			step = 2;
		else
			step  =1;

		// add 1 to result
		result.add((long) 1);

		// start looping up to 1/2 of the number
		for (long i = 1+step; i <= number/2; i+=step)
			if (number % i == 0)
				result.add(i);

		// add number to list
		if (number != 1)
			result.add(number);

		return result;
	}

	/**
	 * Round towards zero.
	 * @param val
	 * @return
	 */
	public static double fix(double val) {
	    if (val < 0) {
	        return Math.ceil(val);
	    }
	    return Math.floor(val);
	}
	
	/** Helper for problem11 that multiplies numbers across a line and returns 0 when it reaches edges.
	 * 
	 * @param doubles The array
	 * @param W The width of the array
	 * @param H The height of the array
	 * @param totalNumbers Total numbers to look for. problem11 is 4
	 * @param i The starting row index
	 * @param j The starting column index
	 * @param horizontalStep The step size in the horizontal direction, so viewing to the right this is 1 for example.
	 * @param verticalStep The step size in the vertical direction, so viewing to the right this is 0 for example.
	 * @return the product
	 * @throws Exception When invalid starting parameters are entered
	 */
	public static double productLine(ArrayList<Double> doubles,
			int W,
			int H,
			int totalNumbers,
			int i,
			int j,
			int horizontalStep,
			int verticalStep) throws Exception{

		// check starting points are within bounds
		if (W*H > doubles.size())
			throw new Exception("W and H are not compatible with size of array");
		if (i<0 || i >= H || j<0 || j>= W)
			throw new Exception("i and j must be within bounds of array");

		// check endpoints are within bounds, no exception though, just return 0
		if (i + verticalStep*(totalNumbers-1) < 0 ||
				i + verticalStep*(totalNumbers-1) >= H ||
				j + horizontalStep*(totalNumbers-1) < 0 ||
				j + horizontalStep*(totalNumbers-1) >= W)
			return 0;

		// do the stepping
		int ii;
		int jj;
		int totalIndex;
		double product = 1;
		for (int index = 0; index<totalNumbers; index++){
			ii = i+index*verticalStep;
			jj = j+index*horizontalStep;
			totalIndex = jj + ii*W;
			product *= doubles.get(totalIndex);
		}

		// return product
		return product;

	}
	
	/**
	 * Round The given number to the specified number of decimals
	 * @param valueToRound The value to round
	 * @param numberOfDecimalPlaces how many decimals we want
	 * @return The new rounded number
	 */
	public static double round(double valueToRound, int numberOfDecimalPlaces){
		
	    double multipicationFactor = Math.pow(10, numberOfDecimalPlaces);
	    double interestedInZeroDPs = valueToRound * multipicationFactor;
	    return Math.round(interestedInZeroDPs) / multipicationFactor;
	}

	/** Check if number is even, simple
	 * 
	 * @param number
	 * @return
	 */
	public static boolean isEven(long number){
		return (number % 2 == 0);
	}

	public static boolean checkStringPalindrome(String input){
		int n = input.length()-1;
		for (int i = 0; i<=Math.floor(input.length()/2); i++){
			if (input.charAt(i) != input.charAt(n-i))
				return false;
		}
		return true;
	}

	/**
	 * Generate list of prime numbers <= number
	 * Taken from matlab primes code
	 * @param number the upper bound
	 * @return the list of primes
	 */
	public static ArrayList <Integer> primes(int number){

		// initialize array
		ArrayList <Integer> result = new ArrayList<Integer>();

		// check if less than 2, then no numbers
		if (number < 2)
			return result;

		// fill array with a large list of number
		result = new ArrayList<Integer>(number/2+1);
		result.add(2);
		for (int i=3; i <= number; i+=2)
			result.add(i);

		// length of array
		int q = result.size();

		//  loop performing some method
		for (int k = 3; k<=Math.sqrt(number); k+=2){
			if (result.get((k+1)/2-1) != 0)
				for (int kk = ((k*k+1)/2); kk <= q; kk+=k)
					result.set(kk-1, 0);
		}

		// only keep elements that are > 0
		Iterator<Integer> itr = result.iterator();
		while (itr.hasNext()){
			if (itr.next() <= 0)
				itr.remove();
		}

		return result;
	}

	/**
	 * returns a vector containing the prime factors of N. Taken from Matlab code factor
	 * @param number the number to factor
	 * @return the prime factors
	 */
	public static ArrayList <Integer> factor(int number){

		// make number positive
		if (number < 0)
			number = -number;

		// special cases
		if (number == 1){
			ArrayList<Integer> result = new ArrayList<Integer>(0);
			return result;
		}
		if (number < 4){
			ArrayList<Integer> result = new ArrayList<Integer>(1);
			result.add(number);
			return result;
		}

		// initialize result
		ArrayList<Integer> result = new ArrayList<Integer>();

		// list of primes <= number
		ArrayList<Integer> primesList = primes((int) Math.sqrt(number));

		int n = number;
		while (n > 1){

			// find index where there is no remainder, basically the primes are a divisor
			ArrayList <Integer> indexNoRem = new ArrayList<Integer>(); 
			for (int i = 0; i<primesList.size(); i++)
				if (n % primesList.get(i) == 0)
					indexNoRem.add(i);

			// no index to break out
			if (indexNoRem.size() == 0){
				result.add(n);
				break;
			}

			// grab only the primesList at index
			@SuppressWarnings("unchecked")
			ArrayList <Integer> tmpPrimesList = (ArrayList<Integer>) primesList.clone();
			primesList.clear();
			primesList.ensureCapacity(indexNoRem.size());
			Iterator<Integer> itr = indexNoRem.iterator();
			int prod=1;
			int value;
			while (itr.hasNext()){
				value = tmpPrimesList.get(itr.next());
				prod *= value;
				primesList.add(value);
			}

			// append new primesList to result
			result.addAll(primesList);

			// reset n
			n = n/prod;
		}

		// sort the results
		Collections.sort(result);
		return result;
	}

	/**
	 * Take an array of Integers and then find all possible combinations of multiples of these numbers. 
	 * Any number of values within the array can be used to find multiples. Only unique numbers are output.
	 * For example, if input = {2, 3, 2, 3}, output is {3, 6, 2, 9, 18, 12, 4, 36}.
	 * Uses recursion
	 * @param inputList
	 * @return
	 */
	public static ArrayList <Integer> allPossibleMultiples(ArrayList <Integer> inputList){

		// copy input list to work on it
		ArrayList<Integer> working = new ArrayList<Integer>(inputList);

		// special case of no more numbers
		if (inputList.size() <= 1)
			return working;

		// recurse taking first number multiplied by multiples of latter numbers
		// remove the first element and save for later
		int first = working.get(0);
		working.remove(0);

		// find all possible multiples for the list of the 2nd term and on
		ArrayList <Integer> output = allPossibleMultiples(working);

		// then new numbers are the first term multiplied by the already found numbers
		int N = output.size();
		for (int i = 0; i < N; i++){

			// add this new multiple to the list if not already present
			int number = output.get(i)*first;
			if (!output.contains(number))
				output.add(number);
		}

		// then add the first term all by itself
		if (!output.contains(first))
			output.add(first);

		return output;
	}


	/** 
	 * n % list, but where list is an arraylist
	 * @param n
	 * @param list
	 * @return
	 */
	public static ArrayList<Integer> mod(int n, ArrayList<Integer> list) {
		ArrayList<Integer> result = new ArrayList<Integer>(list.size());

		Iterator<Integer> itr = list.iterator();
		while (itr.hasNext()){
			result.add(n%itr.next());
		}

		return result;
	}

	/**
	 * Add two strings together as if they were integers
	 * number1 + number2 = sum
	 * @param number1
	 * @param number2
	 * @return the sum
	 */
	public static String addAsString(String number1, String number2){

		// initialize string
		String result = "";

		int i1 = number1.length()-1;
		int i2 = number2.length()-1;
		int carryover = 0;
		int currentNumber;

		// loop across digits starting from the right
		while (i1 >= 0 || i2 >= 0){
			currentNumber = carryover;
			if (i1 >= 0)
				currentNumber += number1.charAt(i1)+CONVERT_CHAR_TO_INT;
			if (i2 >= 0)
				currentNumber += number2.charAt(i2)+CONVERT_CHAR_TO_INT;
			i1--;
			i2--;
			if (currentNumber >= 10)
				carryover = (int) Math.floor(currentNumber/10);
			else
				carryover = 0;

			result += (currentNumber - carryover*10);	
		}

		// add caryover
		if (carryover > 0)
			result += carryover;

		// flip the result
		String result2 = "";
		for (int i = result.length()-1; i >= 0; i--)
			result2 += result.substring(i, i+1);

		return result2;
	}

	/**
	 * Multiply two strings together as if they were integers using elementary school method.
	 * number1*number2 = result
	 * @param number1
	 * @param number2
	 * @return
	 */
	public static String multiplyAsString(String number1, String number2){

		// initialize string and array
		String result = "";
		String[] rows = new String[number2.length()];

		int carryover = 0;
		int currentNumber;
		int ii=0;
		String defaultResult = "";

		// loop across digits starting from the right for smaller number
		for (int i2 = number2.length()-1; i2 >= 0; i2--){

			// initalize numbers
			carryover = 0;
			result = defaultResult;

			// loop across bigger nunber
			for (int i1 = number1.length()-1; i1>= 0; i1--){

				// multiply together and add remainder
				currentNumber = carryover + 
				(number1.charAt(i1)+CONVERT_CHAR_TO_INT) * 
				(number2.charAt(i2)+CONVERT_CHAR_TO_INT);

				// set carryover number
				if (currentNumber >= 10)
					carryover = (int) Math.floor(currentNumber/10);
				else
					carryover = 0;

				// append new string to old result
				result += (currentNumber - carryover*10);	
			}

			// add carryover
			if (carryover > 0)
				result += carryover;

			// flip the result
			String result2 = "";
			for (int i = result.length()-1; i >= 0; i--)
				result2 += result.substring(i, i+1);

			// store the result in string array
			rows[ii] = result2;
			ii++;

			// add a zero to defaultResult
			defaultResult += "0";
		}

		// now add all the rows
		String finalResult = rows[0];
		for (int i = 1; i<rows.length; i++){
			finalResult = addAsString(finalResult, rows[i]);
		}

		return finalResult;
	}

	/**
	 * Calculate the factorial of an integer and return the exact number as a string. No real limit on the 
	 * size of the answer, because it is performed as a string and multipled out as in elementary school.
	 * @param factorialNumber
	 * @return
	 */
	public static String factorialAsString(int factorialNumber){

		// we are going to perform each multiply in string form using old elementary school method
		String currentNumber = "1";
		for (int i = 2; i<=factorialNumber; i++){
			currentNumber = multiplyAsString(currentNumber, String.valueOf(i));
		}

		return currentNumber;
	}

	/**
	 * Calculate day of the week at a particular date. Days are sunday (0), monday (1)...
	 * @param year
	 * @param month
	 * @param day
	 * @return
	 */
	public static int dayOfWeek(int year, int month, int day){

		// helper variables
		int a = (14 - month) / 12;
		int y = year - a;
		int m = month + 12*a - 2;

		//For Julian calendar: d = (5 + day + y + y/4 + (31*m)/12) mod 7
		//For Gregorian calendar: d = (day + y + y/4 - y/100 + y/400 + (31*m)/12) mod 7

		return (day + y + y/4 - y/100 + y/400 + (31*m)/12) % 7;
	}
	
	/**
	 * Perform a matrix multiply of 2 2d matrices
	 * @param m1
	 * @param m2
	 * @return
	 */
	public static double[][] matrixMultiply(double[][] m1, double[][] m2){
		
		// find sizes of arrays
		int m1rows = m1.length;
		int m1cols = m1[0].length;
		int m2rows = m2.length;
		int m2cols = m2[0].length;
		
		// error check that sizes are compatible
		if (m1cols != m2rows)
			throw new IllegalArgumentException("matrices don't match: " + m1cols + " != " + m2rows);
		
		// initialize result
		double[][] result = new double[m1rows][m2cols];

		// multiply
		for (int i=0; i<m1rows; i++)
			for (int j=0; j<m2cols; j++)
				for (int k=0; k<m1cols; k++)
					result[i][j] += m1[i][k] * m2[k][j];

		return result;
	}
	
	/**
	 * Perform a matrix multiply of a 2d matrix, times a 1d array, where the 1d array is assumed to be a row vector
	 * @param m1
	 * @param m2
	 * @return
	 */
	public static double[] matrixMultiply(double[][] m1, double[] m2){
		
		// find sizes of arrays
		int m1rows = m1.length;
		int m1cols = m1[0].length;
		int m2rows = m2.length;
		
		// error check that sizes are compatible
		if (m1cols != m2rows)
			throw new IllegalArgumentException("matrices don't match: " + m1cols + " != " + m2rows);
		
		// initialize result
		double[] result = new double[m1rows];

		// multiply
		for (int i=0; i<m1rows; i++)
			for (int k=0; k<m1cols; k++)
				result[i] += m1[i][k] * m2[k];

		return result;
	}
	
	/**
	 * Transpose a double matrix
	 * @param in
	 * @return
	 */
	public static double[][] transposeMatrix(double[][] in){
		// find sizes of arrays
		int m1rows = in.length;
		int m1cols = in[0].length;
		
		// initialize
		double[][] out = new double[m1cols][m1rows];
		
		// transpose
		for (int i = 0; i < m1cols; i++)
			for (int j = 0; j < m1rows; j++)
				out[i][j] = in[j][i];
		
		return out;	
	}
	
	/**
	 * Take the log base (base) of the number. For example log2(8) = 3, so log(2, 8) = 3.
	 * @param base The base of the logorithm
	 * @param number The number to calculate the logorithm of
	 * @return The log
	 */
	public static double log(double base, double number){
		return Math.log(number)/Math.log(base);
	}
	
	/**
	 * Take the log base 2 of the number. For example log2(8) = 3
	 * @param number The number to calculate the logorithm of
	 * @return The log
	 */
	public static double log2(double number){
		return log(2, number);
	}
	
}
