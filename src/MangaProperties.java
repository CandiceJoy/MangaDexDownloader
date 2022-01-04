/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

public class MangaProperties extends Properties
{
	private static final char[] hexDigit = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
	};
	private final TreeMap<String, String> comments;
	private final ArrayList<String> headers;
	
	public MangaProperties()
	{
		super();
		this.comments = new TreeMap<>();
		this.headers = new ArrayList<>();
	}
	
	private static char toHex( int nibble )
	{
		return hexDigit[( nibble & 0xF )];
	}
	
	public void setProperty( String key, String value, String comment )
	{
		super.setProperty( key, value );
		comments.put( key, comment );
	}
	
	public void store( OutputStream out )
			throws IOException
	{
		store0( new BufferedWriter( new OutputStreamWriter( out, "8859_1" ) ) );
	}
	
	public void addHeader( String header )
	{
		headers.add( header );
	}
	
	private void store0( @NotNull BufferedWriter bw )
			throws IOException
	{
		bw.write( "# " + new Date() );
		bw.newLine();
		
		for( String line : headers )
		{
			bw.write( "# " + line );
			bw.newLine();
		}
		
		bw.newLine();
		synchronized( this )
		{
			for( Enumeration<?> e = keys(); e.hasMoreElements(); )
			{
				String key = (String) e.nextElement();
				String val = (String) get( key );
				key = saveConvert( key, true );
				/* No need to escape embedded and trailing spaces for value, hence
				 * pass false to flag.
				 */
				val = saveConvert( val, false );
				
				String commentline = comments.get( key );
				if( commentline != null )
				{
					bw.write( "# " + commentline );
					bw.newLine();
				}
				bw.write( key + "=" + val );
				bw.newLine();
				
				if( commentline != null )
				{
					bw.newLine();
				}
			}
		}
		bw.flush();
		bw.close();
	}
	
	private @NotNull String saveConvert( @NotNull String theString,
	                                     boolean escapeSpace )
	{
		int len = theString.length();
		int bufLen = len * 2;
		if( bufLen < 0 )
		{
			bufLen = Integer.MAX_VALUE;
		}
		StringBuilder outBuffer = new StringBuilder( bufLen );
		
		for( int x = 0; x < len; x++ )
		{
			char aChar = theString.charAt( x );
			// Handle common case first, selecting largest block that
			// avoids the specials below
			if( ( aChar > 61 ) && ( aChar < 127 ) )
			{
				if( aChar == '\\' )
				{
					outBuffer.append( '\\' );
					outBuffer.append( '\\' );
					continue;
				}
				outBuffer.append( aChar );
				continue;
			}
			switch( aChar )
			{
				case ' ':
					if( x == 0 || escapeSpace )
						outBuffer.append( '\\' );
					outBuffer.append( ' ' );
					break;
				case '\t':
					outBuffer.append( '\\' );
					outBuffer.append( 't' );
					break;
				case '\n':
					outBuffer.append( '\\' );
					outBuffer.append( 'n' );
					break;
				case '\r':
					outBuffer.append( '\\' );
					outBuffer.append( 'r' );
					break;
				case '\f':
					outBuffer.append( '\\' );
					outBuffer.append( 'f' );
					break;
				case '=': // Fall through
				case ':': // Fall through
				case '#': // Fall through
				case '!':
					outBuffer.append( '\\' );
					outBuffer.append( aChar );
					break;
				default:
					if( ( ( aChar < 0x0020 ) || ( aChar > 0x007e ) ) )
					{
						outBuffer.append( '\\' );
						outBuffer.append( 'u' );
						outBuffer.append( toHex( ( aChar >> 12 ) & 0xF ) );
						outBuffer.append( toHex( ( aChar >> 8 ) & 0xF ) );
						outBuffer.append( toHex( ( aChar >> 4 ) & 0xF ) );
						outBuffer.append( toHex( aChar & 0xF ) );
					}
					else
					{
						outBuffer.append( aChar );
					}
			}
		}
		return outBuffer.toString();
	}
}
