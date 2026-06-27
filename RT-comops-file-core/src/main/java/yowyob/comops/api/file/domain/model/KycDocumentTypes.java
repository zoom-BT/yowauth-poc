package yowyob.comops.api.file.domain.model;

import java.util.Locale;
import java.util.Set;

/**
 * Types de documents qui doivent être routés vers le service de vérification d'identité (VerifID).
 *
 * <p>Un upload portant un {@code documentType} appartenant à cet ensemble part en quarantaine
 * (PENDING) et déclenche {@code FILE_ANALYSIS_REQUESTED}. Tout autre fichier (facture, photo,
 * contrat…) est accepté directement sans passer par VerifID.
 */
public final class KycDocumentTypes {

    private static final Set<String> KYC_TYPES = Set.of(
            "ID_CARD",
            "PASSPORT",
            "DRIVER_LICENSE",
            "RESIDENCE_PERMIT");

    private KycDocumentTypes() {
    }

    /** Normalise un documentType libre (trim + upper-case), ou null si vide. */
    public static String normalize(String documentType) {
        if (documentType == null || documentType.isBlank()) {
            return null;
        }
        return documentType.trim().toUpperCase(Locale.ROOT);
    }

    /** Indique si ce documentType nécessite une analyse KYC par VerifID. */
    public static boolean requiresAnalysis(String documentType) {
        String normalized = normalize(documentType);
        return normalized != null && KYC_TYPES.contains(normalized);
    }
}
