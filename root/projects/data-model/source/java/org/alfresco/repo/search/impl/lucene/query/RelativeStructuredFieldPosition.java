/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.search.impl.lucene.query;

import java.io.IOException;

/**
 * Search for a term relative to the last one found.
 * 
 * @author andyh
 */
public class RelativeStructuredFieldPosition extends AbstractStructuredFieldPosition
{

    int relativePosition;

    /**
     * 
     */
    public RelativeStructuredFieldPosition(String termText)
    {
        super(termText.equals("*") ? null : termText, true, false);
        relativePosition = 1;
        
    }

    public RelativeStructuredFieldPosition()
    {
        super(null, false, false);
        relativePosition = 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.lucene.extensions.StructuredFieldPosition#matches(int,
     *      int, org.apache.lucene.index.TermPositions)
     */
    public int matches(int start, int end, int offset) throws IOException
    {

        if (getCachingTermPositions() != null)
        {
            // Doing "termText"
            getCachingTermPositions().reset();
            int count = getCachingTermPositions().freq();
            int requiredPosition = offset + relativePosition;
            int realPosition = 0;
            int adjustedPosition = 0;
            for (int i = 0; i < count; i++)
            {
                realPosition = getCachingTermPositions().nextPosition();
                adjustedPosition = realPosition - start;
                if ((end != -1) && (realPosition > end))
                {
                    return -1;
                }
                if (adjustedPosition == requiredPosition)
                {
                    return adjustedPosition;
                }
                if (adjustedPosition > requiredPosition)
                {
                    return -1;
                }
            }
        }
        else
        {
            // Doing "*";
            return offset + 1;
        }
        return -1;
    }
    
    public String getDescription()
    {
        return "Relative Named child";
    }
}