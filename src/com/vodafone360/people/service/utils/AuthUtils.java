/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at
 * src/com/vodafone360/people/VODAFONE.LICENSE.txt or
 * http://github.com/360/360-Engine-for-Android
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and
 * include the License file at src/com/vodafone360/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the fields
 * enclosed by brackets "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 * Copyright 2010 Vodafone Sales & Services Ltd.  All rights reserved.
 * Use is subject to license terms.
 */

package com.vodafone360.people.service.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.utils.LogUtils;

/**
 * Set of functions that are used for generating 'auth' parameter required for
 * cresting requests and is usually based on a valid session created on
 * sign-up/login.
 */
public class AuthUtils {

    /**
     * Generates a MD5 hash of the input.
     * 
     * @param input - String from which MD5 will be created
     * @return String containing MD5 hash created from input parameter
     * @throws NullPointerException when input is NULL
     * @throws NullPointerException MD5 algorithm could not be found
     */
    public static String getMd5Hash(String input) {
        if (input == null) {
            throw new NullPointerException("AuthUtils.getMd5Hash() input cannot be NULL");
        }
        try {
            String md5 = new BigInteger(1, MessageDigest.getInstance("MD5")
                    .digest(input.getBytes())).toString(16);
            while (md5.length() < 32) {
                md5 = "0" + md5;
            }
            return md5;

        } catch (NoSuchAlgorithmException e) {
            LogUtils
                    .logE("AuthUtils.getMd5Hash() NoSuchAlgorithmException, indicates that MD5 algorithm could not be found.");
            throw new NullPointerException("AuthUtils.getMd5Hash() NoSuchAlgorithmException");
        }
    }

    /**
     * Calculates the AUTH parameter using a list of NameValues. TODO: Why is
     * this called differently for every API?
     * 
     * @param functionName - String representation of function, using the
     *            "class/funcName" convention e.g.
     *            "auth/getsessionbycredentials". Typically empty.
     * @param parameters - List of name/value pairs
     * @param timeStamp - String containing absolute time stamps, generated by
     *            calls to getCurrentTimeInSeconds()
     * @param session - Valid session object or NULL
     * @return AUTH parameter String
     * @throws NullPointerException when functionName is NULL
     * @throws NullPointerException when parameters is NULL
     * @throws NullPointerException when timeStamp is NULL
     */
    public static String calculateAuth(String functionName, Hashtable<String, Object> parameters,
            String timeStamp, AuthSessionHolder session) {
        LogUtils.logI("AuthUtils.calculateAuth() Call to function [" + functionName + "]");
        if (functionName == null) {
            throw new NullPointerException("AuthUtils.calculateAuth() functionName cannot be NULL");
        }
        if (parameters == null) {
            throw new NullPointerException("AuthUtils.calculateAuth() parameters cannot be NULL");
        }
        if (timeStamp == null) {
            throw new NullPointerException("AuthUtils.calculateAuth() timeStamp cannot be NULL");
        }

        StringBuffer toMd5 = new StringBuffer();
        toMd5.append(SettingsManager.getProperty(Settings.APP_SECRET_KEY));
        if (session != null) {
            toMd5.append("&" + session.sessionSecret);
        }
        toMd5.append( "&" + functionName);

        List<String> sortedList = AuthUtils.getSortedListOfKeys(parameters);
        for (int i = 0; i < sortedList.size(); i++) {
            String key = sortedList.get(i);
            toMd5.append("&" + createSigningString(key, parameters.get(key)));
        }

        LogUtils.logI("AuthUtils.calculateAuth() Before auth[" + toMd5 + "]");

        if (session == null) {
            return SettingsManager.getProperty(Settings.APP_KEY_ID) + "::" + timeStamp + "::"
                    + getMd5Hash(toMd5.toString());
        } else {
            return SettingsManager.getProperty(Settings.APP_KEY_ID) + "::" + session.sessionID
                    + "::" + timeStamp + "::" + getMd5Hash(toMd5.toString());
        }
    }

    /**
     * Calculates a signing string from a name/value pair
     * 
     * @param inputString - String name.
     * @param inputObject - Object value can be Hashtable, Vector, byte[] or
     *            String.
     * @return Signing String
     * @throws NullPointerException when inputString is NULL
     * @throws NullPointerException when inputObject is NULL
     */
    @SuppressWarnings("unchecked")
    private static String createSigningString(String inputString, Object inputObject) {
        if (inputString == null) {
            throw new NullPointerException("AuthUtils.createSigning() inputString cannot be NULL");
        }
        if (inputObject == null) {
            throw new NullPointerException("AuthUtils.createSigning() inputObject cannot be NULL");
        }

        if (inputObject instanceof Hashtable<?, ?>) {
            return inputString + "=" + createSignOfHastable((Hashtable<String, Object>)inputObject);

        } else if (inputObject instanceof Vector<?>) {
            return inputString + "=" + createSignOfVector((Vector<Object>)inputObject);

        } else if (inputObject instanceof byte[]) {
            return inputString + "=" + getMd5Hash(createSignOfByteAray((byte[])inputObject));

        } else {
            return inputString + "=" + inputObject.toString();
        }
    }

    /**
     * Calculates the sign value from a Hash table
     * 
     * @param hashTable - Given Hash table
     * @return Sign value String
     * @throws NullPointerException when hashTable is NULL
     */
    private static String createSignOfHastable(Hashtable<String, Object> hashTable) {
        if (hashTable == null) {
            throw new NullPointerException(
                    "AuthUtils.createSignOfHastable() hashTable cannot be NULL");
        }
        StringBuffer returnString = new StringBuffer();
        List<String> list = AuthUtils.getSortedListOfKeys(hashTable);
        for (int i = 0; i < list.size(); i++) {
            String key = list.get(i);
            Object obj = hashTable.get(key);
            returnString.append(createSigningString(key, obj));
            if (i < list.size() - 1) {
                returnString.append("&");
            }
        }
        return returnString.toString();
    }

    /**
     * Calculates the sign value from a Vector
     * 
     * @param hashTable - Given Vector
     * @return Sign value String
     * @throws NullPointerException when vector is NULL
     */
    @SuppressWarnings("unchecked")
    private static String createSignOfVector(Vector<Object> vector) {
        if (vector == null) {
            throw new NullPointerException("AuthUtils.createSignOfByteAray() vector cannot be NULL");
        }
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < vector.size(); i++) {
            Object tempObject = vector.get(i);
            if (tempObject instanceof Hashtable<?, ?>) {
                ret.append(createSignOfHastable((Hashtable<String, Object>) tempObject));

            } else if (tempObject instanceof Vector<?>) {
                ret.append(createSignOfVector((Vector<Object>) tempObject));

            } else {
                ret.append(tempObject.toString());

            }
            if (i < vector.size() - 1) {
                ret.append("&");
            }
        }
        return ret.toString();
    }

    /**
     * Calculates the sign value from a byte array
     * 
     * @param bytes - Given byte array
     * @return Sign value String
     * @throws NullPointerException when bytes is NULL
     */
    private static String createSignOfByteAray(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("AuthUtils.createSignOfByteAray() bytes cannot be NULL");
        }
        return String.valueOf(bytes.length);
    }

    /**
     * Returns a list of keys from the map in alphabetical order
     * 
     * @param map The map to sort
     * @return The ordered list of keys
     */
    private static List<String> getSortedListOfKeys(Hashtable<String, Object> map) {
        List<String> mList = new ArrayList<String>(map.keySet());
        Collections.sort(mList);
        return mList;
    }
}
