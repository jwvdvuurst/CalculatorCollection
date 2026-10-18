export interface Calculator {
    id: number;
    model: string;
    manufacturer: string | null;
    current_price: string;
    price_currency: string;
}

export interface CollectionItem {
    calculator_id: number | null;
    model: string | null;
    notes: string | null;
}

export interface WishlistItem extends CollectionItem {
    manufacturer: string | null;
    added_at: string | null;
}

export interface Manufacturer {
    id: number;
    name: string;
    calculator_count: number;
}

export interface Proposal {
    id: number;
    manufacturer_name: string;
    model: string;
    status: string;
    source_url: string;
    created_by: string;
}

export interface AuthUser {
    username: string;
    email: string;
    role: 'USER' | 'ADMIN';
}

export interface AuthResponse {
    access_token: string;
    token_type: 'bearer';
    expires_in: number;
    user: AuthUser;
}

export interface CalculatorFilters {
    search: string;
    manufacturer_id: string;
    min_price: string;
    max_price: string;
    sort: string;
}

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/+$/, '');
let accessToken: string | null = null;

export const setAccessToken = (token: string | null) => {
    accessToken = token;
};

const isRecord = (value: unknown): value is Record<string, unknown> =>
    typeof value === 'object' && value !== null;

const request = async <T,>(
    path: string,
    options: RequestInit = {},
    query?: URLSearchParams,
): Promise<T> => {
    const url = new URL(`${API_BASE_URL}${path}`, window.location.origin);
    if (query) {
        url.search = query.toString();
    }

    const headers = new Headers(options.headers);
    if (accessToken) {
        headers.set('Authorization', `Bearer ${accessToken}`);
    }
    const response = await fetch(url, { ...options, headers });
    const responseText = await response.text();
    let responseBody: unknown = null;
    if (responseText) {
        try {
            responseBody = JSON.parse(responseText) as unknown;
        } catch {
            responseBody = responseText;
        }
    }

    if (!response.ok) {
        const detail =
            isRecord(responseBody) && typeof responseBody.detail === 'string'
                ? responseBody.detail
                : typeof responseBody === 'string'
                  ? responseBody
                  : `Request failed with status ${response.status}`;
        throw new Error(detail);
    }

    return responseBody as T;
};

const queryFromFilters = (filters: CalculatorFilters): URLSearchParams => {
    const query = new URLSearchParams();
    for (const [key, value] of Object.entries(filters)) {
        if (value.trim()) {
            query.set(key, value.trim());
        }
    }
    return query;
};

export const getCalculators = (filters: CalculatorFilters, signal?: AbortSignal) =>
    request<Calculator[]>('/calculators', { signal }, queryFromFilters(filters));

export const getManufacturers = (signal?: AbortSignal) =>
    request<Manufacturer[]>('/manufacturers', { signal });

export const getCollection = (username: string, signal?: AbortSignal) =>
    request<CollectionItem[]>(`/collection/${encodeURIComponent(username)}`, { signal });

export const getWishlist = (username: string, signal?: AbortSignal) =>
    request<WishlistItem[]>(`/wishlist/${encodeURIComponent(username)}`, { signal });

export const getProposals = (signal?: AbortSignal) =>
    request<Proposal[]>('/proposals', { signal });

const post = <T,>(path: string, payload: unknown) =>
    request<T>(path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    });

export const login = (username: string, password: string) =>
    post<AuthResponse>('/auth/login', { username, password });

export const register = (
    username: string,
    email: string,
    password: string,
    confirmPassword: string,
) =>
    post<AuthResponse>('/auth/register', {
        username,
        email,
        password,
        confirm_password: confirmPassword,
    });

export const addToCollection = (username: string, calculatorId: number) =>
    post(`/collection/${encodeURIComponent(username)}/add`, {
        calculator_id: calculatorId,
        notes: 'Added from UI',
    });

export const addToWishlist = (username: string, calculatorId: number) =>
    post(`/wishlist/${encodeURIComponent(username)}/add`, {
        calculator_id: calculatorId,
        notes: 'Wishlist item',
    });

export const submitProposal = (proposal: {
    manufacturer_name: string;
    model: string;
    source_url: string;
    raw_row_text: string;
}) => post<Proposal>('/proposals', proposal);

export const updateProposalStatus = (proposalId: number, status: 'approved' | 'rejected') =>
    request<{ id: number; status: string }>(`/admin/proposals/${proposalId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status }),
    });
