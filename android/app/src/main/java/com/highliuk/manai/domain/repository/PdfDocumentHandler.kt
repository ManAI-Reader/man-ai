package com.highliuk.manai.domain.repository

import com.highliuk.manai.domain.model.PdfMetadata

/**
 * Abstraction for PDF document operations.
 * Implementation uses ContentResolver + PdfRenderer under the hood.
 */
interface PdfDocumentHandler {
    /**
     * Takes persistable URI permission and extracts metadata from the PDF.
     * @param uriString content:// URI string of the PDF document
     * @return metadata containing title (from filename) and page count
     */
    suspend fun importDocument(uriString: String): PdfMetadata
}
